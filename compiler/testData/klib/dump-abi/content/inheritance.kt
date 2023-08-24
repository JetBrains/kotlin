// MODULE: classifiers_inheritance_library

package classifiers.inheritance

final class FinalClass {
    val finalVal: String get() = ""
    var finalVar: String get() = ""
        set(_) = Unit
    fun finalFun(): String = ""
}

open class OpenClass {
    open val openVal: String get() = ""
    val finalVal: String get() = ""
    open var openVar: String get() = ""
        set(_) = Unit
    var finalVar: String get() = ""
        set(_) = Unit
    open fun openFun(): String = ""
    fun finalFun(): String = ""
}

open class OpenClassImpl1 : OpenClass() {
    override val openVal get() = ""
    override var openVar get() = ""
        set(_) = Unit
    override fun openFun() = ""
}
open class OpenClassImpl2 : OpenClass() {
    final override val openVal get() = ""
    final override var openVar get() = ""
        set(_) = Unit
    final override fun openFun() = ""
}
class OpenClassImpl3 : OpenClass() {
    override val openVal get() = ""
    override var openVar get() = ""
        set(_) = Unit
    override fun openFun() = ""
}
class OpenClassImpl4 : OpenClass() {
    final override val openVal get() = ""
    final override var openVar get() = ""
        set(_) = Unit
    final override fun openFun() = ""
}

abstract class AbstractClass {
    abstract val abstractVal: String
    open val openVal: String get() = ""
    val finalVal: String get() = ""
    abstract var abstractVar: String
    open var openVar: String get() = ""
        set(_) = Unit
    var finalVar: String get() = ""
        set(_) = Unit
    abstract fun abstractFun(): String
    open fun openFun(): String = ""
    fun finalFun(): String = ""
}

abstract class AbstractClassImpl1 : AbstractClass() {
    override val abstractVal get() = ""
    override val openVal get() = ""
    override var abstractVar get() = ""
        set(_) = Unit
    override var openVar get() = ""
        set(_) = Unit
    override fun abstractFun() = ""
    override fun openFun() = ""
}
abstract class AbstractClassImpl2 : AbstractClass() {
    final override val abstractVal get() = ""
    final override val openVal get() = ""
    final override var abstractVar get() = ""
        set(_) = Unit
    final override var openVar get() = ""
        set(_) = Unit
    final override fun abstractFun() = ""
    final override fun openFun() = ""
}
final class AbstractClassImpl3 : AbstractClass() {
    override val abstractVal get() = ""
    override val openVal get() = ""
    override var abstractVar get() = ""
        set(_) = Unit
    override var openVar get() = ""
        set(_) = Unit
    override fun abstractFun() = ""
    override fun openFun() = ""
}
final class AbstractClassImpl4 : AbstractClass() {
    final override val abstractVal get() = ""
    final override val openVal get() = ""
    final override var abstractVar get() = ""
        set(_) = Unit
    final override var openVar get() = ""
        set(_) = Unit
    final override fun abstractFun() = ""
    final override fun openFun() = ""
}

interface Interface1 {
    val abstractVal1: String
    val openVal1: String get() = ""
    fun abstractFun1(): String
    fun openFun1(): String = ""
}

interface Interface2 {
    val abstractVal2: String
    val openVal2: String get() = ""
    fun abstractFun2(): String
    fun openFun2(): String = ""
}

class MultiInheritance_OpenClass_Interface1_Interface2: OpenClass(), Interface1, Interface2 {
    override val abstractVal1 get() = ""
    override val abstractVal2 get() = ""
    override fun abstractFun1() = ""
    override fun abstractFun2() = ""
}

class MultiInheritance_OpenClass_Interface2_Interface1: OpenClass(), Interface2, Interface1 {
    override val abstractVal1 get() = ""
    override val abstractVal2 get() = ""
    override fun abstractFun1() = ""
    override fun abstractFun2() = ""
}

class MultiInheritance_Interface1_OpenClass_Interface2: Interface1, OpenClass(), Interface2 {
    override val abstractVal1 get() = ""
    override val abstractVal2 get() = ""
    override fun abstractFun1() = ""
    override fun abstractFun2() = ""
}

class MultiInheritance_Interface2_OpenClass_Interface1: Interface2, OpenClass(), Interface1 {
    override val abstractVal1 get() = ""
    override val abstractVal2 get() = ""
    override fun abstractFun1() = ""
    override fun abstractFun2() = ""
}

class MultiInheritance_Interface1_Interface2_OpenClass: Interface1, Interface2, OpenClass() {
    override val abstractVal1 get() = ""
    override val abstractVal2 get() = ""
    override fun abstractFun1() = ""
    override fun abstractFun2() = ""
}

class MultiInheritance_Interface2_Interface1_OpenClass: Interface2, Interface1, OpenClass() {
    override val abstractVal1 get() = ""
    override val abstractVal2 get() = ""
    override fun abstractFun1() = ""
    override fun abstractFun2() = ""
}

open class OpenClassWithTypeParameters<P, Q>
class OpenClassWithTypeParametersImpl_Int_String : OpenClassWithTypeParameters<Int, String>()
class OpenClassWithTypeParametersImpl_NInt_NString : OpenClassWithTypeParameters<Int?, String?>()
class OpenClassWithTypeParametersImpl_String_Int : OpenClassWithTypeParameters<String, Int>()
class OpenClassWithTypeParametersImpl_T1_T2<T1, T2> : OpenClassWithTypeParameters<T1, T2>()
class OpenClassWithTypeParametersImpl_T2_T1<T2, T1> : OpenClassWithTypeParameters<T1, T2>()
class OpenClassWithTypeParametersImpl_T1_Number_T2_CS<T1 : Number, T2 : CharSequence> : OpenClassWithTypeParameters<T1, T2>()
class OpenClassWithTypeParametersImpl_DNNT1_DNNT2<T1, T2> : OpenClassWithTypeParameters<T1 & Any, T2 & Any>()

public interface PublicInterface {
    public fun publicDefaultFunInPublicInterface(): String = ""
    private fun privateDefaultFunInPublicInterface(): String = ""
}
internal interface InternalInterface {
    public fun publicDefaultFunInInternalInterface(): String = ""
    private fun privateDefaultFunInInternalInterface(): String = ""
}
private interface PrivateInterface {
    public fun publicDefaultFunInPrivateInterface(): String = ""
    private fun privateDefaultFunInPrivateInterface(): String = ""
}
class ClassThatInheritsDefaultImplsFromInterfaces : PublicInterface, InternalInterface, PrivateInterface

sealed class SealedClass {
    class SealedClass_Subclass1 : SealedClass()
}
class SealedClass_Subclass2 : SealedClass()

sealed interface SealedInterface {
    class SealedInterface_Subclass1 : SealedInterface
}
class SealedInterface_Subclass2 : SealedInterface
