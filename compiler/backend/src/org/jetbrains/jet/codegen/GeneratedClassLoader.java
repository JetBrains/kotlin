package org.jetbrains.jet.codegen;

/**
* @author yole
*/
public class GeneratedClassLoader extends ClassLoader {
    private final ClassFileFactory state;

    public GeneratedClassLoader(ClassFileFactory state) {
        super(GeneratedClassLoader.class.getClassLoader());
        this.state = state;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String file = name.replace('.', '/') + ".class";
        if (state.files().contains(file)) {
            byte[] bytes = state.asBytes(file);
            return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
    }
}
