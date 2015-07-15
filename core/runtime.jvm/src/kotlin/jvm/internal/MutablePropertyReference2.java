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

import kotlin.reflect.KMutableProperty2;
import kotlin.reflect.KProperty2;

public class MutablePropertyReference2 extends MutablePropertyReference implements KMutableProperty2 {
    @Override
    public Object get(Object receiver1, Object receiver2) {
        throw error();
    }

    @Override
    public void set(Object receiver1, Object receiver2, Object value) {
        throw error();
    }

    @Override
    public KProperty2.Getter getGetter() {
        throw error();
    }

    @Override
    public KMutableProperty2.Setter getSetter() {
        throw error();
    }
}
