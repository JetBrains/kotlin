package test

import java.util.ArrayList

open class JavaBeanVarOfGenericType<erased P> {
    open fun getCharacters(): ArrayList<P>? = null
    open fun setCharacters(p0: ArrayList<P>?) { }
    //var characters: ArrayList<P>? = null
}
