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

package quicksort

fun IntArray.swap(i:Int, j:Int) {
    val temp = this[i]
    this[i] = this[j]
    this[j] = temp
}

fun IntArray.quicksort() = quicksort(0, size-1)

fun IntArray.quicksort(L: Int, R:Int) {
    val m = this[(L + R) / 2]
    var i = L
    var j = R
    while (i <= j) {
        while (this[i] < m)
            i++
        while (this[j] > m)
            j--
        if (i <= j) {
            swap(i++, j--)
        }
    }
    if (L < j)
        quicksort(L, j)
    if (R > i)
        quicksort(i, R)
}

fun main(array: Array<String>) {
    val start = System.currentTimeMillis()

    val a = IntArray(100000000)
    var i = 0
    val len = a.size
    while (i < len) {
        a[i] = i * 3 / 2 + 1
        if (i % 3 == 0)
            a[i] = -a[i]
        i++
    }

    a.quicksort()

    val total = System.currentTimeMillis() - start
    System.out?.println("[Quicksort-" + System.getProperty("project.name")+ " Benchmark Result: " + total + "]");
}
