// JET-72 Type inference doesn't work when iterating over ArrayList

import java.util.ArrayList

abstract class Item(val room: Object) {
   abstract val name : String
}

val items: ArrayList<Item> = ArrayList<Item>()

fun test(room : Object) {
  for(item: Item? in items) {
    if (item?.room === room) {
      // item?.room is not null
      System.out.println("You see " + item<!UNNECESSARY_SAFE_CALL!>?.<!>name)
    }
  }
}
