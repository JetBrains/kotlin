// FULL_JDK
import java.lang.ref.WeakReference

fun func1(x: Any) {}

fun func2() {
    func1(WeakReference(Any()).get()!!)
}

// 0 ASTORE
//  ^ no temporary variables created in 'func2'
// 1 ALOAD
//  ^ single ALOAD in 'func1' (parameter null check)

// JVM_IR_TEMPLATES
// 0 checkNotNullExpressionValue
//  ^ no null check on result of 'get()!!'
