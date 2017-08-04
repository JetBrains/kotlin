fun ktTest() {
    Test.<caret>foo("SomeTest")
}

//INFO: <html><head>    <style type="text/css">        #error {            background-color: #eeeeee;            margin-bottom: 10px;        }        p {            margin: 5px 0;        }    </style></head><body><small><b><a href="psi_element://Test"><code>Test</code></a></b></small><PRE><i>@Contract(pure = true)</i>&nbsp;
//INFO: <i>@<a href="psi_element://org.jetbrains.annotations.NotNull"><code>NotNull</code></a></i>&nbsp;
//INFO: public static&nbsp;<a href="psi_element://java.lang.Object"><code>Object</code></a>[]&nbsp;<b>foo</b>(<a href="psi_element://java.lang.String"><code>String</code></a>&nbsp;param)</PRE>
//INFO: <i>Inferred</i> annotations available:<br>
//INFO: <ul>
//INFO: <li><i>@org.jetbrains.annotations.Contract(pure = true)</i> <i>@<a href="psi_element://org.jetbrains.annotations.NotNull"><code>org.jetbrains.annotations.NotNull</code></a></i></li>
//INFO: </ul>
//INFO:
//INFO:        Java Method</body></html>
