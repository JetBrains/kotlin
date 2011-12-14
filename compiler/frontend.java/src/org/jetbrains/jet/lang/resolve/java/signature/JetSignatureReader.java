package org.jetbrains.jet.lang.resolve.java.signature;

import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * @author Stepan Koltsov
 * 
 * @see SignatureReader
 */
public class JetSignatureReader {
    
    private final String signature;

    public JetSignatureReader(String signature) {
        this.signature = signature;
    }


    public void accept(final JetSignatureVisitor v) {
        String signature = this.signature;
        int len = signature.length();
        int pos;
        char c;

        if (signature.charAt(0) == '<') {
            pos = 2;
            do {
                int end = signature.indexOf(':', pos);
                v.visitFormalTypeParameter(signature.substring(pos - 1, end));
                pos = end + 1;

                c = signature.charAt(pos);
                if (c == 'L' || c == '[' || c == 'T') {
                    pos = parseType(signature, pos, v.visitClassBound());
                }

                while ((c = signature.charAt(pos++)) == ':') {
                    pos = parseType(signature, pos, v.visitInterfaceBound());
                }
            } while (c != '>');
        } else {
            pos = 0;
        }

        if (signature.charAt(pos) == '(') {
            pos++;
            while (signature.charAt(pos) != ')') {
                pos = parseType(signature, pos, v.visitParameterType());
            }
            pos = parseType(signature, pos + 1, v.visitReturnType());
            while (pos < len) {
                pos = parseType(signature, pos + 1, v.visitExceptionType());
            }
        } else {
            pos = parseType(signature, pos, v.visitSuperclass());
            while (pos < len) {
                pos = parseType(signature, pos, v.visitInterface());
            }
        }
    }

    public int acceptType(JetSignatureVisitor v) {
        return parseType(this.signature, 0, v);
    }

    public void acceptTypeOnly(JetSignatureVisitor v) {
        int r = acceptType(v);
        if (r != signature.length()) {
            throw new IllegalStateException();
        }
    }


    private static int parseType(
            final String signature,
            int pos,
            final JetSignatureVisitor v)
    {
        char c;
        int start, end;
        boolean visited, inner;
        String name;

        boolean nullable = false;
        if (signature.charAt(pos) == '?') {
            nullable = true;
            pos++;
        }

        switch (c = signature.charAt(pos++)) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
            case 'F':
            case 'J':
            case 'D':
            case 'V':
                v.visitBaseType(c, nullable);
                return pos;

            case '[':
                return parseType(signature, pos, v.visitArrayType(nullable));

            case 'T':
                end = signature.indexOf(';', pos);
                v.visitTypeVariable(signature.substring(pos, end), nullable);
                return end + 1;

            default: // case 'L':
                start = pos;
                visited = false;
                inner = false;
                for (;;) {
                    switch (c = signature.charAt(pos++)) {
                        case '.':
                        case ';':
                            if (!visited) {
                                name = signature.substring(start, pos - 1);
                                if (inner) {
                                    v.visitInnerClassType(name, nullable);
                                } else {
                                    v.visitClassType(name, nullable);
                                }
                            }
                            if (c == ';') {
                                v.visitEnd();
                                return pos;
                            }
                            start = pos;
                            visited = false;
                            inner = true;
                            break;

                        case '<':
                            name = signature.substring(start, pos - 1);
                            if (inner) {
                                v.visitInnerClassType(name, nullable);
                            } else {
                                v.visitClassType(name, nullable);
                            }
                            visited = true;
                            top: for (;;) {
                                switch (c = signature.charAt(pos)) {
                                    case '>':
                                        break top;
                                    case '*':
                                        ++pos;
                                        v.visitTypeArgument();
                                        break;
                                    case '+':
                                    case '-':
                                        pos = parseType(signature,
                                                        pos + 1,
                                                        v.visitTypeArgument(c));
                                        break;
                                    default:
                                        pos = parseType(signature,
                                                        pos,
                                                        v.visitTypeArgument('='));
                                        break;
                                }
                            }
                    }
                }
        }
    }


}
