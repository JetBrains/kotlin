package pack

import java.util.Collection
import java.util.ArrayList
import java.util.regex.Pattern

import std.util.*

class C{
    public fun foo(){
        val items : Collection<Item> = java.util.Collections.singleton(Item()).sure()
        val result = ArrayList<Item>()
        val pattern: Pattern? = Pattern.compile("...")
        items.filterTo(result) {
            pattern.sure().matcher(it.name()).sure().matches()
        }
    }

    private fun Item.name() : String = ""
}

class Item{
}

fun box() : String {
    C().foo()
    return "OK"
}