import testing.KotlinPackageClassUsedFromJava_DataKt;

class KotlinPackageClassUsedFromJava {
    void test() {
        <caret>KotlinPackageClassUsedFromJava_DataKt.foo();
    }
}

//INFO: <html><head>    <style type="text/css">        #error {            background-color: #eeeeee;            margin-bottom: 10px;        }        p {            margin: 5px 0;        }    </style></head><body><small><b>testing</b></small><PRE>public final class <b>testing.KotlinPackageClassUsedFromJava_DataKt</b>
//INFO: extends <a href="psi_element://java.lang.Object"><code>java.lang.Object</code></a></PRE></body></html>
