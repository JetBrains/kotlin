// WITH_REFLECT
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

import kotlin.reflect.KProperty

fun test() {
    // initialization of an immutable local value
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        val baseA: BaseBuildee<TargetType> = this
    }

    // assignment to a mutable local variable
    build {
        var baseB = BaseBuildee<TargetType>()
        baseB = this
    }

    // a destructuring declaration
    build {
        val (_: BaseBuildee<TargetType>, _: BaseBuildee<TargetType>) = this to this
    }

    // body of a getter of an immutable property
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        class LocalWrapper {
            val baseC: BaseBuildee<TargetType>
                get() = this@build
        }
    }

    // initialization of a mutable property's backing field
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        class LocalWrapper {
            var baseD: BaseBuildee<TargetType> = this@build
                set(value) {
                    field = BaseBuildee()
                }
        }
    }

    // body of a setter of a mutable property
    build {
        class LocalWrapper {
            var baseE: BaseBuildee<TargetType> = BaseBuildee()
                set(value) {
                    field = this@build
                }
        }
    }

    // assignment via a mutable property's setter
    build {
        baseF = this
    }

    // declaration of a property delegated via getValue/setValue
    build {
        val baseG: BaseBuildee<TargetType> by Delegate(this)
    }

    // assignment to a property delegated via getValue/setValue
    build {
        baseH = this
    }

    // declaration of a property delegated via provideDelegate
    build {
        val baseI: BaseBuildee<TargetType> by DelegateProvider(this)
    }

    // assignment to a property delegated via provideDelegate
    build {
        baseJ = this
    }

    // an extension property
    build {
        this.baseK
    }
}

open class BaseBuildee<BBTV>
class DerivedBuildee<DBTV>: BaseBuildee<DBTV>()

fun <FTV> build(instructions: DerivedBuildee<FTV>.() -> Unit): DerivedBuildee<FTV> {
    return DerivedBuildee<FTV>().apply(instructions)
}

class TargetType

var baseF
    get() = BaseBuildee<TargetType>()
    set(value) {}

class Delegate<T>(private var arg: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = arg
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        arg = value
    }
}

var baseH by Delegate(BaseBuildee<TargetType>())

class DelegateProvider<T>(private val arg: T) {
    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Delegate<T> = Delegate(arg)
}

var baseJ by DelegateProvider(BaseBuildee<TargetType>())

val BaseBuildee<TargetType>.baseK get() = this
