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

package kotlin.reflect.jvm.internal

import java.lang.reflect.*
import kotlin.reflect.*

interface KPropertyImpl<out R> : KProperty<R>, KCallableImpl<R> {
    val javaField: Field?

    val javaGetter: Method?

    override val getter: Getter<R>

    interface Accessor<out R> : KProperty.Accessor<R> {
        override val property: KPropertyImpl<R>
    }

    interface Getter<out R> : KProperty.Getter<R>, KCallableImpl<R> {
        override val name: String get() = "<get-${property.name}>"
    }
}


interface KMutablePropertyImpl<R> : KMutableProperty<R>, KPropertyImpl<R> {
    val javaSetter: Method?

    override val setter: Setter<R>

    interface Setter<R> : KMutableProperty.Setter<R>, KPropertyImpl.Accessor<R>, KCallableImpl<Unit> {
        override val name: String get() = "<set-${property.name}>"
    }
}
