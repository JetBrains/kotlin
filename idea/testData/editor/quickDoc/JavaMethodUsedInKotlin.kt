fun ktTest() {
    Test.<caret>foo("SomeTest")
}

//INFO: <div class='definition'><pre><a href="psi_element://Test"><code>Test</code></a><br><i>@Contract(value = &quot;_ -&gt; new&quot;,&nbsp;pure = true)</i>&nbsp;
//INFO: <i>@<a href="psi_element://org.jetbrains.annotations.NotNull"><code>NotNull</code></a></i>&nbsp;
//INFO: public static&nbsp;<a href="psi_element://java.lang.Object"><code>Object</code></a>[]&nbsp;<b>foo</b>(<a href="psi_element://java.lang.String"><code>String</code></a>&nbsp;param)</pre></div><div class='content'>
//INFO:        Java Method
//INFO:      <p></div><table class='sections'><p><tr><td valign='top' class='section'><p><i>Inferred</i> annotations:</td><td valign='top'><p><i>@org.jetbrains.annotations.Contract(value = &quot;_ -&gt; new&quot;,&nbsp;pure = true)</i> <i>@<a href="psi_element://org.jetbrains.annotations.NotNull">org.jetbrains.annotations.NotNull</a></i></td></table>
