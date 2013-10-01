/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.results;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.calls.inference.BoundsOwner;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.util.slicedmap.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ResolutionDebugInfo {
    public static final WritableSlice<One, List<? extends ResolutionTask<? extends CallableDescriptor, ?>>> TASKS = Slices.createSimpleSlice();
    public static final WritableSlice<One, ResolvedCall<? extends CallableDescriptor>> RESULT = Slices.createSimpleSlice();

    public static final WritableSlice<ResolvedCall<? extends CallableDescriptor>, StringBuilder> ERRORS = Slices.createSimpleSlice();
    public static final WritableSlice<ResolvedCall<? extends CallableDescriptor>, StringBuilder> LOG = Slices.createSimpleSlice();
    public static final WritableSlice<ResolvedCall<? extends CallableDescriptor>, Map<TypeParameterDescriptor, BoundsOwner>> BOUNDS_FOR_UNKNOWNS = Slices.createSimpleSlice();
    public static final WritableSlice<ResolvedCall<? extends CallableDescriptor>, Map<JetType, BoundsOwner>> BOUNDS_FOR_KNOWNS = Slices.createSimpleSlice();

    public static boolean RESOLUTION_DEBUG_INFO_ENABLED = false;

    public static boolean isResolutionDebugEnabled() {
        Application application = ApplicationManager.getApplication();
        return (RESOLUTION_DEBUG_INFO_ENABLED || application.isInternal()) && !application.isUnitTestMode();
    }

    public static final Data NO_DEBUG_INFO = new AbstractData() {

        @Override
        public String toString() {
            return "NO_DEBUG_INFO";
        }

        @Override
        public <K, V> V getByKey(ReadOnlySlice<K, V> slice, K key) {
            return SlicedMap.DO_NOTHING.get(slice, key);
        }

        @Override
        public <K, V> void putByKey(WritableSlice<K, V> slice, K key, V value) {
        }
    };
    public static final WritableSlice<PsiElement, Data> RESOLUTION_DEBUG_INFO = new BasicWritableSlice<PsiElement, Data>(Slices.ONLY_REWRITE_TO_EQUAL) {
        @Override
        public boolean check(PsiElement key, Data value) {
            return isResolutionDebugEnabled();
        }

        @Override
        public Data computeValue(SlicedMap map, PsiElement key, Data value, boolean valueNotFound) {
            if (valueNotFound) return NO_DEBUG_INFO;
            return super.computeValue(map, key, value, valueNotFound);
        }
    };

    public static Data create() {
        return isResolutionDebugEnabled() ? new DataImpl() : NO_DEBUG_INFO;
    }

    public enum One { KEY }

    public interface Data {
        <K, V> V getByKey(ReadOnlySlice<K, V> slice, K key);
        <K, V> void putByKey(WritableSlice<K, V> slice, K key, V value);

        <V> void set(WritableSlice<One, ? super V> slice, V value);
        <V> V get(ReadOnlySlice<One, V> slice);
    }
    
    private static abstract class AbstractData implements Data {
        @Override
        public <V> void set(WritableSlice<One, ? super V> slice, V value) {
            putByKey(slice, One.KEY, value);
        }

        @Override
        public <V> V get(ReadOnlySlice<One, V> slice) {
            return getByKey(slice, One.KEY);
        }
    }

    private static class DataImpl extends AbstractData {
        private final MutableSlicedMap map = SlicedMapImpl.create();

        @Override
        public <K, V> V getByKey(ReadOnlySlice<K, V> slice, K key) {
            return map.get(slice, key);
        }

        @Override
        public <K, V> void putByKey(WritableSlice<K, V> slice, K key, V value) {
            map.put(slice, key, value);
        }
    }
    
    public static void println(Object message) {
        if (isResolutionDebugEnabled()) {
            System.out.println(message);
        }
    }

    static {
        BasicWritableSlice.initSliceDebugNames(ResolutionDebugInfo.class);
    }
}
