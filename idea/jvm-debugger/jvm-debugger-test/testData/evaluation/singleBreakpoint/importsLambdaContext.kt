package importsLambdaContext

import java.util.Collections
import java.util.ArrayList;
import java.util.HashSet
import java.util.HashMap as JHashMap

fun main(args: Array<String>) {
    //Breakpoint! (lambdaOrdinal = 1)
    arrayOf(1).map { args.size }
}

// EXPRESSION: Collections.emptyList<String>()
// RESULT: instance of java.util.Collections$EmptyList(id=ID): Ljava/util/Collections$EmptyList;

// EXPRESSION: ArrayList<Int>()
// RESULT: instance of java.util.ArrayList(id=ID): Ljava/util/ArrayList;

// EXPRESSION: HashSet<Int>()
// RESULT: instance of java.util.HashSet(id=ID): Ljava/util/HashSet;

// EXPRESSION: JHashMap<Int, Int>()
// RESULT: instance of java.util.HashMap(id=ID): Ljava/util/HashMap;
