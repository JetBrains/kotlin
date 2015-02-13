import k.StaticFieldInClassObjectInTrait;

public class ClassObjectField {
    public static void foo() {
        Object object = StaticFieldInClassObjectInTrait.<caret>XX;
    }
}

// REF: (k.StaticFieldInClassObjectInTrait).XX