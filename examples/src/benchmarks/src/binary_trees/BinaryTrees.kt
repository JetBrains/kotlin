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
package binary_trees;

val minDepth = 4

fun main(args: Array<String>)  {
    val millis = System.currentTimeMillis()

    val n = if (args.size > 0) Integer.parseInt(args[0]) else 20;

    val maxDepth = if (minDepth + 2 > n) minDepth + 2 else n
    val  stretchDepth = maxDepth + 1

    var check = bottomUpTree(0,stretchDepth).itemCheck()
    System.out?.println("stretch tree of depth "+stretchDepth+"\t check: " + check);

    val longLivedTree = bottomUpTree(0,maxDepth);

    var depth = minDepth
    while(depth<=maxDepth){
        val iterations = 1 shl (maxDepth - depth + minDepth)
        check = 0

        for (i in 1..iterations){
            check += bottomUpTree(i,depth).itemCheck()
            check += bottomUpTree(-i,depth).itemCheck()
        }
        System.out?.println("${iterations*2}\t trees of depth $depth\t check: $check")
        depth+=2
    }
    System.out?.println("long lived tree of depth " + maxDepth + "\t check: "+ longLivedTree.itemCheck());

    val total = System.currentTimeMillis() - millis
    System.out?.println("[Binary Trees-" + System.getProperty("project.name") + " Benchmark Result: " + total + "]");
}

fun bottomUpTree(item: Int, depth: Int) : TreeNode =
    if (depth>0){
        TreeNode(item, bottomUpTree(2*item-1, depth-1), bottomUpTree(2*item, depth-1))
    }
    else {
        TreeNode(item)
    }

class TreeNode(val item: Int, val left: TreeNode? = null, val right: TreeNode? = null) {
    fun itemCheck() : Int {
        var res = item
        if(left != null)
            res += left.itemCheck()
        if(right != null)
            res -= right.itemCheck()
        return res
    }
}
