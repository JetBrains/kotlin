/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower

class PathPartInfo(val key: String) {
    var parent: PathPartInfo? = null
    var prev: PathPartInfo? = null
    fun print(
        builder: StringBuilder,
        pathSeparator: String = "/",
        siblingSeparator: String = ":"
    ) = with(builder) {
        var node = this@PathPartInfo
        if (node == ROOT) {
            append("<ROOT>")
            return
        }
        while (node != ROOT) {
            append(pathSeparator)
            append(node.key)
            val key = node.key
            var count = 0
            while (node.prev != null) {
                if (node.prev?.key == key) {
                    count++
                }
                node = node.prev!!
            }
            if (count > 0) {
                append(siblingSeparator)
                append(count)
            }
            node = node.parent ?: ROOT
        }
    }

    override fun toString() = StringBuilder().also { print(it) }.toString()

    companion object {
        val ROOT = PathPartInfo("ROOT")
    }
}

/**
 * This data structure is used to build unique but durable "key paths" for tree structures using a
 * DSL.
 *
 * This is primarily used by the [LiveLiteralTransformer] to create unique and durable keys for
 * all of the constant literals in an IR source tree.
 */
class DurableKeyVisitor(private var keys: MutableSet<String> = mutableSetOf()) {
    private var current: PathPartInfo = PathPartInfo.ROOT
    private var parent: PathPartInfo? = null
    private var sibling: PathPartInfo? = null

    /**
     * Enter into a new scope with path part [part].
     */
    fun <T> enter(part: String, block: () -> T): T {
        val prev = current
        val prevSibling = sibling
        val prevParent = parent
        val next = PathPartInfo(part)
        try {
            when {
                prevParent != null && prevSibling == null -> {
                    next.parent = prevParent
                    sibling = next
                    parent = null
                }
                prevParent != null && prevSibling != null -> {
                    next.prev = prevSibling
                    sibling = next
                    parent = null
                }
                else -> {
                    next.parent = prev
                    parent = null
                }
            }
            current = next
            return block()
        } finally {
            current = prev
            parent = prevParent
        }
    }

    /**
     * Inside this block, treat all entered path parts as siblings of the current path part.
     */
    fun <T> siblings(block: () -> T): T {
        if (parent != null) {
            // we are already in a siblings block, so we want this to be a no-op
            return block()
        }
        val prevSibling = sibling
        val prevParent = parent
        val prevCurrent = current
        try {
            parent = current
            sibling = null
            return block()
        } finally {
            parent = prevParent
            sibling = prevSibling
            current = prevCurrent
        }
    }

    /**
     * Enter into a new scope with path part [part] and assume entered paths to be children of
     * that path.
     *
     * This is shorthand for `enter(part) { siblings(block) } }`.
     */
    fun <T> siblings(part: String, block: () -> T): T = enter(part) { siblings(block) }

    /**
     * This API is meant to allow for a sub-hierarchy of the tree to be treated as its own scope.
     * This will use the provided [keys] Set as the container for keys that are built while in
     * this scope. Inside of this scope, the previous scope will be completely ignored.
     */
    fun <T> root(
        keys: MutableSet<String> = mutableSetOf(),
        block: () -> T
    ): T {
        val prevKeys = this.keys
        val prevCurrent = current
        val prevParent = parent
        val prevSibling = sibling
        try {
            this.keys = keys
            current =
                PathPartInfo.ROOT
            parent = null
            sibling = null
            return siblings(block)
        } finally {
            this.keys = prevKeys
            current = prevCurrent
            parent = prevParent
            sibling = prevSibling
        }
    }

    /**
     * Build a path at the current position in the tree.
     *
     * @param prefix A string to prefix the path with
     * @param pathSeparator The string used to separate parts of the path
     * @param siblingSeparator When duplicate siblings are found an incrementing index is used to
     * make the path unique. This string will be used to separate the path part from the
     * incrementing index.
     *
     * @return A pair with `first` being the built key, and `second` being whether or not the key
     * was absent in the dictionary of already built keys. If `second` is false, this key is a
     * duplicate.
     */
    fun buildPath(
        prefix: String,
        pathSeparator: String = "/",
        siblingSeparator: String = ":"
    ): Pair<String, Boolean> {
        return buildString {
            append(prefix)
            current.print(this, pathSeparator, siblingSeparator)
        }.let {
            it to keys.add(it)
        }
    }
}