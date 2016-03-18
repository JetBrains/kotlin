import k.StaticFieldInClassObjectInTrait;

public class ClassObjectField {
    public static void foo() {
        Object object = StaticFieldInClassObjectInTrait.<caret>XX;
    }
}

// REF: (in k.StaticFieldInClassObjectInTrait.Companion).XX