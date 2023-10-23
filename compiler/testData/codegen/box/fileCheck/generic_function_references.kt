// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlin.reflect.KFunction2

fun <StringifyTP> stringify(collection: StringifyTP, size: (StringifyTP) -> Int, get: StringifyTP.(Int) -> Any?): String {
    var res = "["
    for (i in 0 until size(collection)) {
        if (i > 0) res += ", "
        res += collection.get(i).toString()
    }
    res += "]"
    return res
}

interface I

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#stringifyArray(kotlin.Array<0:0>){0\C2\A7<I>}kotlin.String"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader** {{%[0-9]+}})
fun <StringifyArrayTP : I> stringifyArray(array: Array<StringifyArrayTP>) =
        // CHECK: call %struct.ObjHeader* @"kfun:#stringify(0:0;kotlin.Function1<0:0,kotlin.Int>;kotlin.Function2<0:0,kotlin.Int,kotlin.Any?>){0\C2\A7<kotlin.Any?>}kotlin.String"
        stringify(
                array,
                { it.size }, // $stringifyArray$lambda$0$FUNCTION_REFERENCE$0
                Array<*>::get // $get$FUNCTION_REFERENCE$1
        )

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#stringifyIntArray(kotlin.Array<kotlin.Int>){}kotlin.String"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader** {{%[0-9]+}})
fun stringifyIntArray(array: Array<Int>) =
        // CHECK: call %struct.ObjHeader* @"kfun:#stringify(0:0;kotlin.Function1<0:0,kotlin.Int>;kotlin.Function2<0:0,kotlin.Int,kotlin.Any?>){0\C2\A7<kotlin.Any?>}kotlin.String"
        stringify(
                array,
                { it.size }, // $stringifyIntArray$lambda$1$FUNCTION_REFERENCE$2
                Array<Int>::get // $get$FUNCTION_REFERENCE$3
        )

class N(val v: Int) : I {
    override fun toString() = v.toString()
}

@Suppress("UNUSED_PARAMETER")
fun <BazTP0, BazTP1> foo(p1: BazTP0, p2: BazTP1) {}

fun <QuxTP> bar() {
    val ref: KFunction2<QuxTP, QuxTP, Unit> = ::foo // $foo$FUNCTION_REFERENCE$4
    println(ref)
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    println(stringifyArray(arrayOf(N(2), N(14))))
    println(stringifyIntArray(arrayOf(1, 2, 3)))

    bar<Int>()
    bar<String>()

    val ref: KFunction2<Int, Int, Unit> = ::foo // $foo$FUNCTION_REFERENCE$5
    println(ref)
    return "OK"
}

// CHECK-LABEL: define internal i32 @"kfun:$stringifyArray$lambda$0$FUNCTION_REFERENCE$0.invoke#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:$stringifyArray$lambda$0$FUNCTION_REFERENCE$0.<init>#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal %struct.ObjHeader* @"kfun:$stringifyArray$lambda$0$FUNCTION_REFERENCE$0.$<bridge-BNNN>invoke(kotlin.Array<1:0>){}kotlin.Int#internal"
// CHECK-SAME: (%struct.ObjHeader* [[this:%[0-9]+]], %struct.ObjHeader* [[array:%[0-9]+]], %struct.ObjHeader** {{%[0-9]+}})
// CHECK-OPT: call i32 @"kfun:$stringifyArray$lambda$0$FUNCTION_REFERENCE$0.invoke#internal"(%struct.ObjHeader* [[this]], %struct.ObjHeader* [[array]])
// CHECK-DEBUG: call i32 @"kfun:$stringifyArray$lambda$0$FUNCTION_REFERENCE$0.invoke#internal"(%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal %struct.ObjHeader* @"kfun:$get$FUNCTION_REFERENCE$1.invoke#internal"
// CHECK-SAME: (%struct.ObjHeader* [[this:%[0-9]+]], %struct.ObjHeader* [[array:%[0-9]+]], i32 [[index:%[0-9]+]], %struct.ObjHeader** [[ret:%[0-9]+]])
// CHECK-OPT: call %struct.ObjHeader* @Kotlin_Array_get(%struct.ObjHeader* [[array]], i32 [[index]], %struct.ObjHeader** [[ret]])
// CHECK-DEBUG: call %struct.ObjHeader* @Kotlin_Array_get(%struct.ObjHeader* {{%[0-9]+}}, i32 {{%[0-9]+}}, %struct.ObjHeader** {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:$get$FUNCTION_REFERENCE$1.<init>#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal %struct.ObjHeader* @"kfun:$get$FUNCTION_REFERENCE$1.$<bridge-NNNNB>invoke(kotlin.Array<*>;kotlin.Int){}kotlin.Any?#internal"
// CHECK-SAME: (%struct.ObjHeader* [[this:%[0-9]+]], %struct.ObjHeader* [[array:%[0-9]+]], %struct.ObjHeader* [[boxedIndex:%[0-9]+]], %struct.ObjHeader** [[ret:%[0-9]+]])
// CHECK-OPT: call %struct.ObjHeader* @"kfun:$get$FUNCTION_REFERENCE$1.invoke#internal"(%struct.ObjHeader* [[this]], %struct.ObjHeader* [[array]], i32 {{%[0-9]+}}, %struct.ObjHeader** [[ret]])
// CHECK-DEBUG: call %struct.ObjHeader* @"kfun:$get$FUNCTION_REFERENCE$1.invoke#internal"(%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}}, i32 {{%[0-9]+}}, %struct.ObjHeader** {{%[0-9]+}})

// CHECK-LABEL: define internal i32 @"kfun:$stringifyIntArray$lambda$1$FUNCTION_REFERENCE$2.invoke#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:$stringifyIntArray$lambda$1$FUNCTION_REFERENCE$2.<init>#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal %struct.ObjHeader* @"kfun:$stringifyIntArray$lambda$1$FUNCTION_REFERENCE$2.$<bridge-BNNN>invoke(kotlin.Array<kotlin.Int>){}kotlin.Int#internal"
// CHECK-SAME: (%struct.ObjHeader* [[this:%[0-9]+]], %struct.ObjHeader* [[array:%[0-9]+]], %struct.ObjHeader** {{%[0-9]+}})
// CHECK-OPT: call i32 @"kfun:$stringifyIntArray$lambda$1$FUNCTION_REFERENCE$2.invoke#internal"(%struct.ObjHeader* [[this]], %struct.ObjHeader* [[array]])
// CHECK-DEBUG: call i32 @"kfun:$stringifyIntArray$lambda$1$FUNCTION_REFERENCE$2.invoke#internal"(%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal i32 @"kfun:$get$FUNCTION_REFERENCE$3.invoke#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}}, i32 {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:$get$FUNCTION_REFERENCE$3.<init>#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal %struct.ObjHeader* @"kfun:$get$FUNCTION_REFERENCE$3.$<bridge-BNNNB>invoke(kotlin.Array<kotlin.Int>;kotlin.Int){}kotlin.Int#internal"
// CHECK-SAME: (%struct.ObjHeader* [[this:%[0-9]+]], %struct.ObjHeader* [[array:%[0-9]+]], %struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader** {{%[0-9]+}})
// CHECK-OPT: call i32 @"kfun:$get$FUNCTION_REFERENCE$3.invoke#internal"(%struct.ObjHeader* [[this]], %struct.ObjHeader* [[array]], i32 {{%[0-9]+}})
// CHECK-DEBUG: call i32 @"kfun:$get$FUNCTION_REFERENCE$3.invoke#internal"(%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}}, i32 {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:$foo$FUNCTION_REFERENCE$4.invoke#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* [[p1:%[0-9]+]], %struct.ObjHeader* [[p2:%[0-9]+]])
// CHECK-OPT: call void @"kfun:#foo(0:0;0:1){0\C2\A7<kotlin.Any?>;1\C2\A7<kotlin.Any?>}"(%struct.ObjHeader* [[p1]], %struct.ObjHeader* [[p2]])
// CHECK-DEBUG: call void @"kfun:#foo(0:0;0:1){0\C2\A7<kotlin.Any?>;1\C2\A7<kotlin.Any?>}"(%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:$foo$FUNCTION_REFERENCE$4.<init>#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal %struct.ObjHeader* @"kfun:$foo$FUNCTION_REFERENCE$4.$<bridge-UNNNN>invoke(1:0;1:0){}#internal"
// CHECK-SAME: (%struct.ObjHeader* [[this:%[0-9]+]], %struct.ObjHeader* [[p1:%[0-9]+]], %struct.ObjHeader* [[p2:%[0-9]+]], %struct.ObjHeader** {{%[0-9]+}})
// CHECK-OPT: call void @"kfun:$foo$FUNCTION_REFERENCE$4.invoke#internal"(%struct.ObjHeader* [[this]], %struct.ObjHeader* [[p1]], %struct.ObjHeader* [[p2]])
// CHECK-DEBUG: call void @"kfun:$foo$FUNCTION_REFERENCE$4.invoke#internal"(%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:$foo$FUNCTION_REFERENCE$5.invoke#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, i32 {{%[0-9]+}}, i32 {{%[0-9]+}})
// CHECK: call void @"kfun:#foo(0:0;0:1){0\C2\A7<kotlin.Any?>;1\C2\A7<kotlin.Any?>}"(%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:$foo$FUNCTION_REFERENCE$5.<init>#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal %struct.ObjHeader* @"kfun:$foo$FUNCTION_REFERENCE$5.$<bridge-UNNBB>invoke(kotlin.Int;kotlin.Int){}#internal"
// CHECK-SAME: (%struct.ObjHeader* [[this:%[0-9]+]], %struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader** {{%[0-9]+}})
// CHECK-OPT: call void @"kfun:$foo$FUNCTION_REFERENCE$5.invoke#internal"(%struct.ObjHeader* [[this]], i32 {{%[0-9]+}}, i32 {{%[0-9]+}})
// CHECK-DEBUG: call void @"kfun:$foo$FUNCTION_REFERENCE$5.invoke#internal"(%struct.ObjHeader* {{%[0-9]+}}, i32 {{%[0-9]+}}, i32 {{%[0-9]+}})
