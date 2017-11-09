/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.support.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Denotes that the annotated element should only be called on the given API level
 * or higher.
 * <p>
 * This is similar in purpose to the older {@code @TargetApi} annotation, but more
 * clearly expresses that this is a requirement on the caller, rather than being
 * used to "suppress" warnings within the method that exceed the {@code minSdkVersion}.
 */
@Retention(CLASS)
@Target({TYPE,METHOD,CONSTRUCTOR,FIELD})
public @interface RequiresApi {
    /**
     * The API level to require. Alias for {@link #api} which allows you to leave out the
     * {@code api=} part.
     */
    @IntRange(from=1)
    int value() default 1;

    /** The API level to require */
    @IntRange(from=1)
    int api() default 1;
}
