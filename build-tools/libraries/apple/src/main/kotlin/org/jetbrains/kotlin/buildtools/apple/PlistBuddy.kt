/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path
import java.nio.file.Paths

/**
 * `/usr/libexec/PlistBuddy`, which reads and edits property lists.
 *
 * Commands are passed with `-c`. Keys and values are quoted and escaped, so quotes, backslashes, colons in keys and
 * non-ASCII text reach the plist unchanged.
 */
public class PlistBuddy internal constructor(private val tools: AppleTools) {

    /** `PlistBuddy -c <command>... <plist>`. A missing [plist] is created by the first command that writes. */
    public fun invocation(plist: Path, commands: List<PlistBuddyCommand>): AppleToolInvocation {
        require(commands.isNotEmpty()) { "PlistBuddy needs at least one command" }
        val arguments = commands.flatMap { listOf("-c", it.render()) } + plist.toString()
        return tools.systemInvocation(PLIST_BUDDY, arguments)
    }

    /**
     * Runs [invocation] and returns its output, which holds the result of [PlistBuddyCommand.Print] commands.
     *
     * PlistBuddy runs every command even after one fails and then exits with 1. With [ignoreFailures] set, that exit
     * code is accepted, which is useful to delete entries that may not exist.
     */
    public fun run(plist: Path, commands: List<PlistBuddyCommand>, ignoreFailures: Boolean = false): String {
        val invocation = invocation(plist, commands).let { if (ignoreFailures) it.copy(acceptedExitCodes = setOf(0, 1)) else it }
        return tools.runner.runChecked(invocation).stdout
    }

    private companion object {
        val PLIST_BUDDY: Path = Paths.get("/usr/libexec/PlistBuddy")
    }
}

/**
 * A PlistBuddy command. Entries are addressed by a key path: `listOf("CFBundleSupportedPlatforms", "0")` is the first
 * element of that array. An empty key path is the root.
 */
public sealed interface PlistBuddyCommand {
    /** The command text passed to `-c`. */
    public fun render(): String

    /** Adds the entry at [keyPath]. [value] is ignored for [PlistValueType.DICT] and [PlistValueType.ARRAY]. */
    public class Add(
        public val keyPath: List<String>,
        public val type: PlistValueType,
        public val value: String? = null,
    ) : PlistBuddyCommand {
        init {
            require(keyPath.isNotEmpty()) { "Add needs a key path" }
            require(value != null || type.isContainer) { "Add of a ${type.keyword} needs a value" }
        }

        override fun render(): String = buildString {
            append("Add ").append(renderKeyPath(keyPath)).append(' ').append(type.keyword)
            if (!type.isContainer) append(' ').append(renderValue(value!!))
        }
    }

    /** Sets the entry at [keyPath] to [value]. */
    public class Set(public val keyPath: List<String>, public val value: String) : PlistBuddyCommand {
        init {
            require(keyPath.isNotEmpty()) { "Set needs a key path" }
        }

        override fun render(): String = "Set ${renderKeyPath(keyPath)} ${renderValue(value)}"
    }

    /** Deletes the entry at [keyPath]. */
    public class Delete(public val keyPath: List<String>) : PlistBuddyCommand {
        init {
            require(keyPath.isNotEmpty()) { "Delete needs a key path" }
        }

        override fun render(): String = "Delete ${renderKeyPath(keyPath)}"
    }

    /** Prints the entry at [keyPath], or the whole plist for an empty key path. */
    public class Print(public val keyPath: List<String> = emptyList()) : PlistBuddyCommand {
        override fun render(): String = if (keyPath.isEmpty()) "Print" else "Print ${renderKeyPath(keyPath)}"
    }
}

/** Value types PlistBuddy can add. */
public enum class PlistValueType(
    /** The type name in a PlistBuddy command. */
    public val keyword: String,
) {
    /** A string. */
    STRING("string"),

    /** An integer. */
    INTEGER("integer"),

    /** A floating point number. */
    REAL("real"),

    /** `true` or `false`. */
    BOOL("bool"),

    /** A date. */
    DATE("date"),

    /** Binary data. */
    DATA("data"),

    /** An empty dictionary. */
    DICT("dict"),

    /** An empty array. */
    ARRAY("array"),
    ;

    internal val isContainer: Boolean get() = this == DICT || this == ARRAY
}

private fun renderKeyPath(keyPath: List<String>): String =
    "\":" + keyPath.joinToString(":") { it.escapedForPlistBuddy().replace(":", "\\:") } + "\""

private fun renderValue(value: String): String = "\"" + value.escapedForPlistBuddy() + "\""

// Inside a quoted PlistBuddy token, a backslash escapes the next character, and both quote kinds end the token.
private fun String.escapedForPlistBuddy(): String =
    replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
