fun f() {
    val lambda_1 = <!ELEMENT!>@ {}

    val lambda_2 = <!ELEMENT!>@ {
        println(1)
    }

    val lambda_3 = @someAnotation <!ELEMENT!>@ {
        println(1)
    }

    val lambda_4 = @someAnotation1 @someAnotation2 @someAnotation3 <!ELEMENT!>@ {
        println(1)
    }

    val x1 = <!ELEMENT!>@ 10 - 1
    val x2 = <!ELEMENT!>@(listOf(1))
    val x3 = <!ELEMENT!>@(return return) && <!ELEMENT!>@ return return
    val x4 = <!ELEMENT!>@ try {} finally {} && <!ELEMENT!>@ return return
    val x5 = <!ELEMENT!>@ try { false } catch(e: E) {} catch (e: Exception) { true } && <!ELEMENT!>@ when (value) { <!ELEMENT!>@ true -> <!ELEMENT!>@ false; <!ELEMENT!>@ false -> <!ELEMENT!>@ true }
}
