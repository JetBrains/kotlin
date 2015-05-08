// "Create class 'Foo'" "true"
// ERROR: <html>Type inference failed: <table><tr><td width="10%"></td><td align="right" colspan="2" style="white-space:nowrap;font-weight:bold;"><b>constructor</b> Foo&lt;U&gt;</td><td style="white-space:nowrap;font-weight:bold;">(</td><td align="right" style="white-space:nowrap;font-weight:bold;">u: U</td><td style="white-space:nowrap;font-weight:bold;">)</td><td style="white-space:nowrap;font-weight:bold;"></td></tr><tr><td colspan="7" style="white-space:nowrap;">cannot be applied to</td></tr><tr><td width="10%"></td><td style="white-space:nowrap;"></td><td style="white-space:nowrap;"></td><td style="white-space:nowrap;"><b>(</b></td><td align="right" style="white-space:nowrap;"><font color=red><b>U</b></font></td><td style="white-space:nowrap;"><b>)</b></td></tr></table></html>
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>U</td></tr><tr><td>Found:</td><td>U</td></tr></table></html>

class A<T>(val n: T) {

}

fun test<U>(u: U) {
    val a = A(u).<caret>Foo(u)
}