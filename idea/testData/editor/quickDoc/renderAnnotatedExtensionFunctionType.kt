fun myFun<caret>(param: @MyAnnotation (String.() -> Unit)) {} // quick documentation myFun

@Target(AnnotationTarget.TYPE)
annotation class MyAnnotation

//INFO: <div class='definition'><pre><font color="808080"><i>renderAnnotatedExtensionFunctionType.kt</i></font><br>public fun <b>myFun</b>(
//INFO:     param: @<a href="psi_element://MyAnnotation">MyAnnotation</a>()
//INFO: (String.() &rarr; Unit)
//INFO: ): Unit</pre></div></pre></div><table class='sections'><p></table>
