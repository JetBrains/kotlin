@CompileTimeCalculation
abstract class BaseIterator(var baseStart: Int) {
    fun baseNext(): Int {
        baseStart += 1
        return base()
    }

    abstract fun base(): Int
}

@CompileTimeCalculation
abstract class ComplexIterator(var complexStart: Int) : BaseIterator(complexStart) {
    fun complexNext() = abstractCall()

    private fun abstractCall(): Int {
        complexStart *= 2
        return complex() + baseNext()
    }

    abstract fun complex(): Int
}

@CompileTimeCalculation
class ImplementIterator constructor(val i: Int) : ComplexIterator(i) {
    override fun complex(): Int {
        return complexStart
    }

    override fun base(): Int {
        return baseStart
    }
}

@CompileTimeCalculation
fun getIterator(i: Int): ComplexIterator = ImplementIterator(i)

const val next = <!EVALUATED: `31`!>getIterator(10).complexNext()<!>
