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

package kotlin.reflect

public trait KMemberExtensionProperty<T : Any, E, out R> : KProperty<R> {
    public fun get(instance: T, extensionReceiver: E): R
}

public trait KMutableMemberExtensionProperty<T : Any, E, R> : KMemberExtensionProperty<T, E, R>, KMutableProperty<R> {
    public fun set(instance: T, extensionReceiver: E, value: R)
}
