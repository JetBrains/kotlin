// TARGET_BACKEND: JVM_IR
// ISSUE: KT-65111

// FILE: plugin/Plugin.java

package plugin;

import entities.*;

public class Plugin {
    public CommonFields getCommonFields() {
        return new CommonFields("OK");
    }
}

// FILE: entities/CommonFields.kt

package entities

data class CommonFields(val screenShots: String)

// FILE: test/foo.kt

package test

import plugin.Plugin

fun foo(plugin: Plugin): String? {
    return plugin.commonFields?.screenShots
}

// FILE: example/test.kt

import test.foo
import plugin.Plugin

fun box(): String =
    foo(Plugin())!!
