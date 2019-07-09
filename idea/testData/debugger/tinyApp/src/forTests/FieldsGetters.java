package forTests;

public class FieldsGetters {
    public static class PublicField {
        public String foo = "a";
    }

    public static class PackagePrivateField {
        String foo = "b";
    }

    public static class ProtectedField {
        protected String foo = "c";
    }

    public static class PrivateField {
        private String foo = "d";
    }

    public static class PublicFieldGetter {
        public final String foo = "a";

        public String getFoo() {
            return "b";
        }
    }

    public static class PrivateFieldPublicGetter {
        private final String foo = "c";

        public String getFoo() {
            return "d";
        }
    }

    public static class PrivateFieldPrivateGetter {
        private final String foo = "e";

        public String getFoo() {
            return "f";
        }
    }

    public static class PublicGetter1 extends PublicField {
        public String getFoo() {
            return "g";
        }
    }

    public static class PublicGetter2 extends PackagePrivateField {
        public String getFoo() {
            return "h";
        }
    }

    public static class PrivateGetter1 extends PrivateField {
        private String getFoo() {
            return "g";
        }
    }

    public static class PublicGetterOnly {
        public String getFoo() {
            return "a";
        }
    }

    public static class PublicFieldAndGetterInParent extends PublicGetterOnly {
        public String foo = "b";
    }

    public abstract class AbstractGetter {
        public String foo = "c";
        public abstract String getFoo();
    }
}