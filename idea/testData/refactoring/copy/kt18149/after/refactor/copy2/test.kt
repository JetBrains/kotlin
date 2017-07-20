package refactor.copy2

import refactor.ParentJava
import refactor.copy.Company
import kotlin.properties.Delegates

enum class Possible {
    NO, YES
}

data class Potable(val p1: String)
class Insider(val peace: String)
class Init {
    fun referred() = 0
    fun moved() = referred()
}

interface FaceToFace {
    fun extract()
}

abstract class AbsToAbs {
    abstract fun extract()
}

class Simple {
    fun extract() {}
}

annotation class MemAnn
class Variety<C> {
    // object
    object ExtractedObject {}
    // function
    tailrec fun tailrecFun(p: Int) { if (p > 0) tailrecFun(p - 1) }
    operator fun plus(other: Variety<C>) {}
    infix fun infixFun(other: Variety<C>) {}
    inline fun inlineFun(f: () -> Int) = f()
    external fun externalFun()
    internal fun internalFun() = 0
    protected fun protectedFun() = 0
    private fun privateFun() = 0
    fun <T> genFunB(p: T): T = p
    fun <T> genFunC(p: T): C where T : C = p
    @MemAnn
    fun annotatedFun() = 0
    final fun finalFun() = 0
    // property
    var publicProp = 0
    internal var internalProp = 0
    protected var protectedProp = 0
    private var privateProp = 0
    var <T> List<T>.genVarJ: T
        get() = last()
        set(p) {}
    var <T> List<T>.genVarL: T where T : C
        get() = last()
        set(p) {}
    @MemAnn
    val annotatedVal = 0
    var byVar by Delegates.notNull<Int>()
    lateinit var lateVal: String
    final val finalVal = 0
    // class
    class ExtractedClass {}
    // inner class
    inner class ExtractedInnerClass {}
    // anonymousInitializer
    init {}
    // secondaryConstructor
    constructor(p: Int)

    fun refer(p1: List<String>) {
        val v1 = ExtractedObject
        val v2 = ExtractedClass()
        val v3 = ExtractedInnerClass()
        val v4 = tailrecFun(2)
        val v5 = this + this
        val v6 = this infixFun this
        val v7 = inlineFun { 7 }
        val vv = externalFun()
        val v8 = internalFun()
        val v9 = protectedFun()
        val vA = privateFun()
        val vB = genFunB(0)
        val vD = annotatedFun()
        val vw = finalFun()
        publicProp = publicProp + 1
        internalProp = internalProp + 1
        protectedProp = protectedProp + 1
        privateProp = privateProp + 1
        p1.genVarJ = p1.genVarJ + "+"
        val vK = annotatedVal
        byVar = byVar + 1
        lateVal = lateVal + "+"
        val vN = finalVal
    }
}

fun <C> Variety<C>.extend() {}

object VarietyObject {
    const val constProp = 0

    fun refer() {
        val vE = constProp
    }
}

abstract class AbstractVariety {
    abstract fun abstractFun(): Int
    abstract var abstractVar: Int
    fun refer() = abstractFun() + abstractVar
}

interface FaceVarietyObligation {
    fun abstractToAbstract()
    fun abstractToConcrete()
    fun concreteToConcrete()
}

interface FaceVariety : FaceVarietyObligation {
    fun abstractFun()
    var abstractProperty: String
    fun concreteFun() {}
    override fun abstractToAbstract()
    override fun abstractToConcrete() {}
    override fun concreteToConcrete() {}
}

@Suppress("LeakingThis")
open class CtorParameter(pp: String, open val pv: String, open var pr: String) {
    var vb = pp + pv + pr
    init { vb += pp + pv + pr }
    constructor() : this("p", "v", "r")
    fun refer() {
        pr += pv
        vb += pr
    }
    companion object {
        val instance = CtorParameter()
    }
}

class CtorParameterChild(val pvc: String, var prc: String) : CtorParameter(pvc, pvc, prc)
class CtorParameterChild2: CtorParameter {
    constructor() : super("", "", "")
}

class CtorParameterChild3(override val pv: String, override var pr: String) : CtorParameter(pv, pv, pr)
data class CtorData(val pv: String, var pr: String) {}
class Company {
    companion object {
        val companyVal = 0
        var companyVar = 0
        fun companyFun() = 0
        class CompanyClass {}
        object CompanyObject {}
    }

    fun refer() {
        println(companyVal)
        companyVar = companyVar + 1
        println(companyFun())
        val v1 = CompanyClass()
        val v2 = CompanyObject
    }
}

fun referCompany() {
    println(Company.companyVal)
    Company.companyVar = Company.companyVar + 1
    println(Company.companyFun())
    val v1 = Company.Companion.CompanyClass()
    val v2 = Company.Companion.CompanyObject
}

class JavaChild : ParentJava.FaceJava {
    override fun inherit() {}
}