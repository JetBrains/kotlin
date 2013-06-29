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

package org.jetbrains.jet.preloading.instrumentation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodInterceptor {
    // JVM internal name like java/util/Map$Entry or short name like FooBar
    String className();

    // regexp, if omitted, field name is used
    String methodName() default "";

    // regexp for method descriptor, like (ILjava/lang/Object;)V for void foo(int, Object)
    String methodDesc() default "";

    // if this is false, an exception is thrown when more than one method in the same class matches
    boolean allowMultipleMatches() default false;

    // if true, every method instrumented with this interceptor will be logged to stdout
    boolean logInterceptions() default false;

    // if true, byte codes of every method instrumented with this interceptor will be logged to stdout
    boolean dumpByteCode() default false;
}
