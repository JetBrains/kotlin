// FIR_IDENTICAL

// JET-72 Type inference doesn't work when iterating over ArrayList

import java.util.ArrayList

abstract class Item(val room: <warning>Object</warning>) {
   abstract val name : String
}

val items: ArrayList<Item> = ArrayList<Item>()

fun test(room : <warning>Object</warning>) {
  for(item: Item in items) {
    if (item.room === room) {
      System.out.println("You see " + item.name)
    }
  }
}
