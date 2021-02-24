/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.tasks;

public enum ExplicitReceiverKind {
    EXTENSION_RECEIVER,
    DISPATCH_RECEIVER,
    NO_EXPLICIT_RECEIVER,

    // A very special case.
    // In a call 'b.foo(1)' where class 'Foo' has an extension member 'fun B.invoke(Int)' function 'invoke' has two explicit receivers:
    // 'b' (as extension receiver) and 'foo' (as dispatch receiver).
    BOTH_RECEIVERS;

    public boolean isExtensionReceiver() {
        return this == EXTENSION_RECEIVER || this == BOTH_RECEIVERS;
    }

    public boolean isDispatchReceiver() {
        return this == DISPATCH_RECEIVER || this == BOTH_RECEIVERS;
    }
}
