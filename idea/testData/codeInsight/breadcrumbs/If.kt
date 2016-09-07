fun foo() {
    if (x) {
        if (x.y) {
            if (y) {

            }
            else {
                if (a) {

                }
                else if (b) {

                }
                else if (c) {
                    if (q) {

                    }
                    else if (qq) {

                    }
                    else if (qqq) {

                    }
                    else {
                        if (p) <caret>return
                    }
                }
                else {

                }
            }
        }
    }
}