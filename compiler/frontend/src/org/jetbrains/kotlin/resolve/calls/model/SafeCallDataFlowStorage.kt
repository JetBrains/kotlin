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

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Deque

/**
 * This class is intended to store data flow information bound to different safe calls
 * in a chain like x?.foo()?.bar() or nested chains like x?.foo(y?.gav()?.mau())?.bar(z?.biz(y?.gav())).
 * Chains can also include simple calls like foo() or qualified calls like x.foo(),
 * but they are not a point of interest here.
 *
 * The typical use-case is: <ul>
 *     <li>begin a new call chain when a new set of call arguments like y?.gav() for foo(y?.gav()) is visited</li>
 *     <li>end a call chain when call arguments analysis is finished</li>
 *     <li>begin receiver analysis when a new safe receiver like y?. is entered</li>
 *     <li>end receiver analysis and give result when a safe receiver is left</li>
 *     <li>at the end of safe qualified expression analysis, check whether a current chain is over,
 *     if yes, get and clear data flow information from the first receiver in the chain</li>
 * </ul>
 * For example like bis(x!!)?.foo()?.bar() we will do receiver analysis for bis(x!!) and bis(x!!)?.foo().
 * Then we take x!=null from bis(x!!) and proceed with this to successive statements.
 */
public class SafeCallDataFlowStorage {

    private val storage: Deque<Array<DataFlowInfo?>> = ArrayDeque()

    private val lengthStack: Deque<Int> = ArrayDeque()

    private var firstReceiverInfo: DataFlowInfo?
        get() = storage.peek()[0]
        set(info: DataFlowInfo?) {
            storage.peek()[0] = info
        }

    init {
        beginChain()
    }

    /**
     * Call this function to begin a new call chain
     */
    public fun beginChain() {
        storage.push(Array(1, { null }))
        lengthStack.push(0)
    }

    /**
     * Call this function to end a call chain and delete all information about it
     */
    public fun endChain() {
        lengthStack.pop()
        storage.pop()
        if (storage.isEmpty() || lengthStack.isEmpty()) {
            throw IllegalStateException("Unexpected safe call data flow storage underflow")
        }
    }

    /**
     * Call this function to start new receiver analysis
     */
    public fun beginReceiverAnalysis() {
        lengthStack.push(lengthStack.pop() + 1)
    }

    /**
     * Call this function to end receiver analysis and give result
     */
    public fun endReceiverAnalysis(result: DataFlowInfo) {
        lengthStack.push(lengthStack.pop() - 1)
        if (firstReceiverInfo == null) {
            firstReceiverInfo = result
        }
    }


    /**
     * Call this function when a current safe call chain is over,
     * to get data flow information from the first receiver in a chain,
     * and simultaneously clear information about this safe call chain
     */
    public fun getAndClearFirstReceiver(): DataFlowInfo {
        if (!chainOver()) {
            throw IllegalStateException("Attempt to get and clear first receiver not at the end of safe call chain")
        }
        val result = firstReceiverInfo
        if (result == null) {
            throw IllegalStateException("Unexpected absence of first receiver data flow info")
        }
        firstReceiverInfo = null
        return result
    }

    /**
     * Call this function to check whether a current safe call chain is over
     */
    public fun chainOver(): Boolean = lengthStack.peek() == 0
}
