/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.tasks;

public enum ExplicitReceiverKind {
    RECEIVER_ARGUMENT,
    THIS_OBJECT,
    NO_EXPLICIT_RECEIVER,

    // A very special case.
    // In a call 'b.foo(1)' where class 'Foo' has an extension member 'fun B.invoke(Int)' function 'invoke' has two explicit receivers:
    // 'b' (as receiver argument) and 'foo' (as this object).
    BOTH_RECEIVERS;

    public boolean isReceiver() {
        return this == RECEIVER_ARGUMENT || this == BOTH_RECEIVERS;
    }

    public boolean isThisObject() {
        return this == THIS_OBJECT || this == BOTH_RECEIVERS;
    }
}
