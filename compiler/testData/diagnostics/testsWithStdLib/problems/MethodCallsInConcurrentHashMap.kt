// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87967
// FULL_JDK

import java.util.concurrent.ConcurrentHashMap

fun <K, V> foo(map: ConcurrentHashMap<String, Int>, genericMap: ConcurrentHashMap<K, V>, nullableMap: ConcurrentHashMap<String?, Int?>) {
    val x1 = map.putIfAbsent(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    val x2 = genericMap.putIfAbsent(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    val x3 = nullableMap.putIfAbsent(null, null)

    map.replace(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    genericMap.replace(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    nullableMap.replace(null, null, null)

    val y1 = map.replace(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    val y2 = genericMap.replace(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    val y3 = nullableMap.replace(null, null)

    map.replaceAll { k: String?, v: Int? ->
        <!NULL_FOR_NONNULL_TYPE!>null<!>
    }
    genericMap.replaceAll { k: K?, v: V? ->
        <!NULL_FOR_NONNULL_TYPE!>null<!>
    }
    nullableMap.replaceAll { k: String?, v: Int? ->
        null
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, lambdaLiteral, localProperty, nullableType, outProjection,
propertyDeclaration, samConversion */
