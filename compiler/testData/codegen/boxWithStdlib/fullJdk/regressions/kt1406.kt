package pack

import java.util.ArrayList
import java.util.regex.Pattern

import kotlin.util.*

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
