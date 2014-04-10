package imports

import java.util.Collections
import java.util.ArrayList;
import java.util.HashSet
import java.util.HashMap as JHashMap

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

// EXPRESSION: Collections.emptyList<String>()
// RESULT: instance of java.util.Collections$EmptyList(id=337): Ljava/util/Collections$EmptyList;

// EXPRESSION: ArrayList<Int>()
// RESULT: instance of java.util.ArrayList(id=383): Ljava/util/ArrayList;

// EXPRESSION: HashSet<Int>()
// RESULT: instance of java.util.HashSet(id=387): Ljava/util/HashSet;

// EXPRESSION: JHashMap<Int, Int>()
// RESULT: instance of java.util.HashMap(id=391): Ljava/util/HashMap;
