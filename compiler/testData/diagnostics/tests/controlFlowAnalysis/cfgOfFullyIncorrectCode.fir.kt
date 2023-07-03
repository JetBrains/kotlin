// ISSUE: KT-47494

<!SYNTAX!><!>{

    try {
        with("",{ {


            <!RETURN_NOT_ALLOWED!>return<!>

        } }<!SYNTAX!><!>

    }

    finally


        <!SYNTAX!><!>try {}

        finally<!SYNTAX!><!>
