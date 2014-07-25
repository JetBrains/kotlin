/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.storage;

import kotlin.Function0;
import kotlin.Function1;
import kotlin.Unit;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.util.ReenteringLazyValueComputationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class StorageManagerTest extends TestCase {

    private StorageManager m;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        m = new LockBasedStorageManager();
    }

    public static <T> void doTestComputesOnce(Function0<T> v, T expected, Counter counter) throws Exception {
        assert 0 == counter.getCount();

        T result1 = v.invoke();
        T result2 = v.invoke();
        assertEquals(1, counter.getCount());
        assertEquals(expected, result1);
        assertEquals(result1, result2);
    }

    public static <T> void doTestExceptionPreserved(Function0<T> v, Class<? extends Throwable> expected, Counter counter)
            throws Exception {
        assert 0 == counter.getCount();

        Throwable caught1 = null;
        try {
            v.invoke();
            fail();
        }
        catch (Throwable e) {
            caught1 = e;
        }
        Throwable caught2 = null;
        try {
            v.invoke();
            fail();
        }
        catch (Throwable e) {
            caught2 = e;
        }

        assertEquals(1, counter.getCount());
        assertTrue("Wrong exception class: " + caught1, expected.isInstance(caught1));
        assertSame(caught1, caught2);
    }

    // Functions


    public void testIsComputed() throws Exception {
        NotNullLazyValue<String> value = m.createLazyValue(new CounterValue());
        assertFalse(value.isComputed());
        value.invoke();
        assertTrue(value.isComputed());
    }

    public void testIsNullableComputed() throws Exception {
        NullableLazyValue<String> value = m.createNullableLazyValue(new CounterValueNull());
        assertFalse(value.isComputed());
        value.invoke();
        assertTrue(value.isComputed());
    }

    public void testIsComputedAfterException() throws Exception {
        NotNullLazyValue<String> value = m.createLazyValue(new ExceptionCounterValue());
        assertFalse(value.isComputed());

        try {
            value.invoke();
        }
        catch (Exception ignored) {
        }

        assertTrue(value.isComputed());
    }

    public void testIsNullableComputedAfterException() throws Exception {
        NullableLazyValue<String> value = m.createNullableLazyValue(new ExceptionCounterValue());
        assertFalse(value.isComputed());

        try {
            value.invoke();
        }
        catch (Exception ignored) {
        }

        assertTrue(value.isComputed());
    }

    public void testFunctionComputesOnce() throws Exception {
        CounterFunction counter = new CounterFunction();
        MemoizedFunctionToNotNull<String, String> f = m.createMemoizedFunction(counter);
        doTestComputesOnce(apply(f, "ok"), "ok1", counter);
    }

    public void testNullableFunctionComputesOnce() throws Exception {
        CounterFunction counter = new CounterFunction();
        MemoizedFunctionToNullable<String, String> f = m.createMemoizedFunctionWithNullableValues(counter);
        doTestComputesOnce(apply(f, "ok"), "ok1", counter);
    }

    public void testNullIsNotConfusedForNotComputedInFunction() throws Exception {
        CounterFunctionToNull counter = new CounterFunctionToNull();
        MemoizedFunctionToNullable<String, String> f = m.createMemoizedFunctionWithNullableValues(counter);
        doTestComputesOnce(apply(f, ""), null, counter);
    }

    public void testFunctionPreservesExceptions() throws Exception {
        ExceptionCounterFunction counter = new ExceptionCounterFunction();
        MemoizedFunctionToNotNull<String, String> f = m.createMemoizedFunction(counter);
        doTestExceptionPreserved(apply(f, ""), UnsupportedOperationException.class, counter);
    }

    public void testNullableFunctionPreservesExceptions() throws Exception {
        ExceptionCounterFunction counter = new ExceptionCounterFunction();
        MemoizedFunctionToNullable<String, String> f = m.createMemoizedFunctionWithNullableValues(counter);
        doTestExceptionPreserved(apply(f, ""), UnsupportedOperationException.class, counter);
    }

    public void testRecursionDetection() throws Exception {
        class C {
            MemoizedFunctionToNotNull<String, String> rec = m.createMemoizedFunction(
                    new Function1<String, String>() {
                        @Override
                        public String invoke(String s) {
                            return rec.invoke("!!!");
                        }
                    }
            );
        }

        try {
            new C().rec.invoke("");
            fail();
        }
        catch (AssertionError e) {
            assertTrue(e.getMessage().startsWith("Recursion detected on input: !!!"));
        }
    }

    // Values

    public void testNotNullLazyComputedOnce() throws Exception {
        CounterValue counter = new CounterValue();
        NotNullLazyValue<String> value = m.createLazyValue(counter);
        doTestComputesOnce(value, "ok1", counter);
    }

    public void testNullableLazyComputedOnce() throws Exception {
        CounterValue counter = new CounterValue();
        NullableLazyValue<String> value = m.createNullableLazyValue(counter);
        doTestComputesOnce(value, "ok1", counter);
    }

    public void testNullIsNotConfusedForNotComputed() throws Exception {
        CounterValueNull counter = new CounterValueNull();
        NullableLazyValue<String> value = m.createNullableLazyValue(counter);
        doTestComputesOnce(value, null, counter);
    }

    public void testNotNullLazyPreservesException() throws Exception {
        ExceptionCounterValue counter = new ExceptionCounterValue();
        NotNullLazyValue<String> value = m.createLazyValue(counter);
        doTestExceptionPreserved(value, UnsupportedOperationException.class, counter);
    }

    public void testNullableLazyPreservesException() throws Exception {
        ExceptionCounterValue counter = new ExceptionCounterValue();
        NullableLazyValue<String> value = m.createNullableLazyValue(counter);
        doTestExceptionPreserved(value, UnsupportedOperationException.class, counter);
    }

    public void testRecursionIntolerance() throws Exception {
        class C {
            NotNullLazyValue<String> rec = m.createLazyValue(new Function0<String>() {
                @Override
                public String invoke() {
                    return rec.invoke();
                }
            });
        }

        try {
            new C().rec.invoke();
            fail();
        }
        catch (IllegalStateException e) {
            // OK
        }
    }

    public void testNullableRecursionIntolerance() throws Exception {
        class C {
            NullableLazyValue<String> rec = m.createNullableLazyValue(new Function0<String>() {
                @Override
                public String invoke() {
                    return rec.invoke();
                }
            });
        }

        try {
            new C().rec.invoke();
            fail();
        }
        catch (IllegalStateException e) {
            // OK
        }
    }

    public void testRecursionTolerance() throws Exception {
        class C {
            NotNullLazyValue<String> rec = m.createRecursionTolerantLazyValue(new Function0<String>() {
                @Override
                public String invoke() {
                    assertEquals("rec", rec.invoke());
                    return "tolerant!";
                }
            }, "rec");
        }

        assertEquals("tolerant!", new C().rec.invoke());
    }

    public void testNullableRecursionTolerance() throws Exception {
        class C {
            NullableLazyValue<String> rec = m.createRecursionTolerantNullableLazyValue(new Function0<String>() {
                @Override
                public String invoke() {
                    assertEquals(null, rec.invoke());
                    return "tolerant!";
                }
            }, null);
        }

        assertEquals("tolerant!", new C().rec.invoke());
    }

    public void testRecursionIntoleranceWithPostCompute() throws Exception {
        @SuppressWarnings("unchecked")
        class C {
            NotNullLazyValue<String> rec = m.createLazyValueWithPostCompute(
                    new Function0<String>() {
                        @Override
                        public String invoke() {
                            return rec.invoke();
                        }
                    },
                    null,
                    new Function1<String, Unit>() {
                        @Override
                        public Unit invoke(String s) {
                            return Unit.INSTANCE$;
                        }
                    }
            );
        }

        try {
            new C().rec.invoke();
            fail();
        }
        catch (IllegalStateException e) {
            // OK
        }
    }

    public void testRecursionToleranceAndPostCompute() throws Exception {
        final CounterImpl counter = new CounterImpl();
        class C {
            NotNullLazyValue<String> rec = m.createLazyValueWithPostCompute(
                    new Function0<String>() {
                        @Override
                        public String invoke() {
                            return rec.invoke();
                        }
                    },
                    new Function1<Boolean, String>() {
                        @Override
                        public String invoke(Boolean aBoolean) {
                            return "tolerant";
                        }
                    },
                    new Function1<String, Unit>() {
                        @Override
                        public Unit invoke(String s) {
                            counter.inc();
                            assertEquals("tolerant", s);
                            return Unit.INSTANCE$;
                        }
                    }
            );
        }

        C c = new C();
        assertEquals("tolerant", c.rec.invoke());
        c.rec.invoke();
        assertEquals("postCompute() called more than once", 1, counter.getCount());
    }

    public void testPostComputeNoRecursion() throws Exception {
        final CounterImpl counter = new CounterImpl();
        NotNullLazyValue<Collection<String>> v = m.createLazyValueWithPostCompute(
                new Function0<Collection<String>>() {
                    @Override
                    public Collection<String> invoke() {
                        List<String> strings = new ArrayList<String>();
                        strings.add("first");
                        return strings;
                    }
                },
                null,
                new Function1<Collection<String>, Unit>() {
                    @Override
                    public Unit invoke(Collection<String> strings) {
                        counter.inc();
                        strings.add("postComputed");
                        return Unit.INSTANCE$;
                    }
                }
        );

        assertEquals(Arrays.asList("first", "postComputed"), v.invoke());
        v.invoke();
        assertEquals(1, counter.getCount());
    }

    public void testNullablePostComputeNoRecursion() throws Exception {
        final CounterImpl counter = new CounterImpl();
        NullableLazyValue<Collection<String>> v = m.createNullableLazyValueWithPostCompute(
                new Function0<Collection<String>>() {
                    @Override
                    public Collection<String> invoke() {
                        ArrayList<String> strings = new ArrayList<String>();
                        strings.add("first");
                        return strings;
                    }
                },
                new Function1<Collection<String>, Unit>() {
                    @Override
                    public Unit invoke(Collection<String> strings) {
                        counter.inc();
                        strings.add("postComputed");
                        return Unit.INSTANCE$;
                    }
                }
        );

        assertEquals(Arrays.asList("first", "postComputed"), v.invoke());
        v.invoke();
        assertEquals(1, counter.getCount());
    }

    public void testRecursionPreventionWithDefaultOnSecondRun() throws Exception {
        @SuppressWarnings("unchecked")
        class C {
            NotNullLazyValue<String> rec = m.createLazyValueWithPostCompute(
                    new Function0<String>() {
                        @Override
                        public String invoke() {
                            return rec.invoke();
                        }
                    },
                    new Function1<Boolean, String>() {
                        @Override
                        public String invoke(Boolean firstTime) {
                            if (firstTime) {
                                throw new ReenteringLazyValueComputationException();
                            }
                            return "second";
                        }
                    },
                    new Function1<String, Unit>() {
                        @Override
                        public Unit invoke(String s) {
                            fail("Recursion-tolerating value should not be post computed");
                            return Unit.INSTANCE$;
                        }
                    }
            );
        }

        C c = new C();
        try {
            c.rec.invoke();
            fail();
        }
        catch (ReenteringLazyValueComputationException e) {
            // OK
        }

        assertEquals("second", c.rec.invoke());
    }

    public void testFallThrough() throws Exception {
        final CounterImpl c = new CounterImpl();
        class C {
            NotNullLazyValue<Integer> rec = LockBasedStorageManager.NO_LOCKS.createLazyValue(new Function0<Integer>() {
                @Override
                public Integer invoke() {
                    c.inc();
                    if (c.getCount() < 2) {
                        return rec.invoke();
                    }
                    else {
                        return c.getCount();
                    }
                }
            });
        }

        assertEquals(2, new C().rec.invoke().intValue());
        assertEquals(2, c.getCount());
    }

    // ExceptionHandlingStrategy

    public void testExceptionHandlingStrategyForLazyValues() throws Exception {
        class RethrownException extends RuntimeException {}

        LockBasedStorageManager m = LockBasedStorageManager.createWithExceptionHandling(new LockBasedStorageManager.ExceptionHandlingStrategy() {
            @NotNull
            @Override
            public RuntimeException handleException(@NotNull Throwable throwable) {
                throw new RethrownException();
            }
        });
        try {
            m.createLazyValue(
                    new Function0<Object>() {
                        @Nullable
                        @Override
                        public Object invoke() {
                            throw new RuntimeException();
                        }
                    }
            ).invoke();
            fail("Exception should have occurred");
        }
        catch (RethrownException ignored) {
        }
    }

    public void testExceptionHandlingStrategyForMemoizedFunctions() throws Exception {
        class RethrownException extends RuntimeException {}

        LockBasedStorageManager m = LockBasedStorageManager.createWithExceptionHandling(new LockBasedStorageManager.ExceptionHandlingStrategy() {
            @NotNull
            @Override
            public RuntimeException handleException(@NotNull Throwable throwable) {
                throw new RethrownException();
            }
        });
        try {
            m.createMemoizedFunction(
                    new Function1<Object, Object>() {
                        @Nullable
                        @Override
                        public Object invoke(@Nullable Object o) {
                            throw new RuntimeException();
                        }
                    }
            ).invoke("");
            fail("Exception should have occurred");
        }
        catch (RethrownException ignored) {
        }
    }

    // toString()

    public void testToString() throws Exception {
        assertTrue("Should mention the setUp() method of this class: " + m.toString(),
                   m.toString().contains(getClass().getSimpleName() + ".setUp("));
    }

    // Utilities

    private static <K, V> Function0<V> apply(final Function1<K, V> f, final K x) {
        return new Function0<V>() {
            @Override
            public V invoke() {
                return f.invoke(x);
            }
        };
    }

    private interface Counter {
        int getCount();
    }

    private static class CounterImpl implements Counter {

        private int count;

        public void inc() {
            count++;
        }

        @Override
        public int getCount() {
            return count;
        }
    }

    private static class CounterValueNull extends CounterImpl implements Function0<String>, Counter {
        @Override
        public String invoke() {
            inc();
            return null;
        }
    }

    private static class CounterValue extends CounterValueNull {
        @Override
        public String invoke() {
            inc();
            return "ok" + getCount();
        }
    }

    private static class ExceptionCounterValue extends CounterValueNull {
        @Override
        public String invoke() {
            inc();
            throw new UnsupportedOperationException();
        }
    }

    private static class CounterFunctionToNull extends CounterImpl implements Function1<String, String>, Counter {
        @Override
        public String invoke(String s) {
            inc();
            return null;
        }
    }

    private static class CounterFunction extends CounterFunctionToNull {
        @Override
        public String invoke(String s) {
            inc();
            return s + getCount();
        }
    }

    private static class ExceptionCounterFunction extends CounterFunctionToNull {
        @Override
        public String invoke(String s) {
            inc();
            throw new UnsupportedOperationException();
        }
    }
}
