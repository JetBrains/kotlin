package test

import java.util.ArrayList

public open class JavaBeanVarOfGenericType<P>() : java.lang.Object() {
    public open fun getCharacters(): ArrayList<P>? = null
    public open fun setCharacters(p0: ArrayList<P>?) { }
    //var characters: ArrayList<P>? = null
}
