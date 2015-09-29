/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.jvm.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KotlinSyntheticClass {
    @Deprecated
    int abiVersion();

    int[] version() default {};

    Kind kind();

    enum Kind {
        PACKAGE_PART,
        TRAIT_IMPL,
        LOCAL_TRAIT_IMPL,
        SAM_WRAPPER,
        SAM_LAMBDA,
        CALLABLE_REFERENCE_WRAPPER,
        LOCAL_FUNCTION,
        ANONYMOUS_FUNCTION,
        WHEN_ON_ENUM_MAPPINGS,
    }
}
