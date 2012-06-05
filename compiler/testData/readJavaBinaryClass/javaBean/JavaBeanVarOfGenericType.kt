package test

import java.util.ArrayList

open class JavaBeanVarOfGenericType<P>() : java.lang.Object() {
    open fun getCharacters(): ArrayList<P>? = null
    open fun setCharacters(p0: ArrayList<P>?) { }
    //var characters: ArrayList<P>? = null
}
