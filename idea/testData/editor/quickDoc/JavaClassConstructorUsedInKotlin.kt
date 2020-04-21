fun testing() {
    SomeClassWithParen("param", 1<caret>)
}

//INFO: <div class='definition'><pre>public class <b>SomeClass</b>&lt;T extends <a href="psi_element://java.util.List"><code>List</code></a>&gt;
//INFO: extends <a href="psi_element://java.lang.Object"><code>Object</code></a></pre></div><div class='content'>
//INFO:    Some Java Class
//INFO:    </div><table class='sections'><p><tr><td valign='top' class='section'><p>Type parameters:</td><td valign='top'>&lt;T&gt; &ndash; </td></table>
