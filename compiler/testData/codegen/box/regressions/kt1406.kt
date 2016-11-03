// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME
// FULL_JDK

package pack

import java.util.ArrayList
import java.util.regex.Pattern

class C{
    public fun foo(){
        val items : Collection<Item> = java.util.Collections.singleton(Item())!!
        val result = ArrayList<Item>()
        val pattern: Pattern? = Pattern.compile("...")
        items.filterTo(result) {
            pattern!!.matcher(it.name())!!.matches()
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
