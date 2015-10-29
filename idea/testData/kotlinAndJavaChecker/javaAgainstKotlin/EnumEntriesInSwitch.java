package test;

class EnumEntriesInSwitch {
    void foo() {
        ZZZ a = ZZZ.A1;
        switch (a) {
            case A1: break;
            case B1: break;
        }

        switch (ZZZ.B1) {
            case A1: break;
            case B1: break;
        }
    }
}
