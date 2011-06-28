package org.jetbrains.jet.grammar;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author abreslav
 */
public class ConfluenceHyperlinksGenerator {

    public static void main(String[] args) throws IOException {
        String grammarExtension = "grm";
        File grammarDir = new File("grammar/src");

        List<String> fileNamesInOrder = Arrays.asList(
                "notation",
                "toplevel",
                "class",
                "class_members",
                "enum",
                "types",
                "modifiers",
                "attributes",
                "control",
                "expressions",
                "when",
                "lexical"
            );

        StringBuilder allRules = new StringBuilder();
        Set<File> used = new HashSet<File>();
        for (String fileName : fileNamesInOrder) {
            File file = new File(grammarDir, fileName + "." + grammarExtension);
            used.add(file);
            FileReader fileReader = new FileReader(file);
            try {
                int c;
                while ((c = fileReader.read()) != -1) {
                    allRules.append((char) c);
                }
                allRules.append("\n");
                allRules.append("\n");
            } finally {
                fileReader.close();
            }
        }

        for (File file : grammarDir.listFiles()) {
            if (file.getName().endsWith(grammarExtension)) {
                if (!used.contains(file)) {
                    throw new IllegalStateException("Unused grammar file : " + file.getAbsolutePath());
                }
            }
        }

        StringBuilder output = new StringBuilder();

        Pattern symbolReference = Pattern.compile("^\\w+$", Pattern.MULTILINE);
        Matcher matcher = symbolReference.matcher(allRules);
        int copiedUntil = 0;
        while (matcher.find()) {
            int start = matcher.start();
            output.append(allRules.subSequence(copiedUntil, start));

            String group = matcher.group();
            output.append("&").append(group);
            copiedUntil = matcher.end();
        }
        output.append(allRules.subSequence(copiedUntil, allRules.length()));
//        System.out.println(output);

        _GrammarLexer grammarLexer = new _GrammarLexer((Reader) null);
        grammarLexer
                .reset(output, 0, output.length(), 0);

        StringBuilder result = new StringBuilder("h1. Contents\n").append("{toc:style=disc|indent=20px}");

        Set<String> declaredSymbols = new HashSet<String>();
        Set<String> usedSymbols = new HashSet<String>();
        List<Token> tokens = new ArrayList<Token>();

        while (true) {
            Token advance = grammarLexer.advance();
            if (advance == null) {
                break;
            }
            if (advance instanceof Declaration) {
                Declaration declaration = (Declaration) advance;
                declaredSymbols.add(declaration.getName());
            }
            else if (advance instanceof Identifier) {
                Identifier identifier = (Identifier) advance;
                usedSymbols.add(identifier.getName());
            }
            tokens.add(advance);
        }

        for (Token token : tokens) {
            if (token instanceof Declaration) {
                Declaration declaration = (Declaration) token;
                if (!usedSymbols.contains(declaration.getName())) {
//                    result.append("(!) *Unused!* ");
                    System.out.println("Unused: " + token);
                }
            }
            else if (token instanceof Identifier) {
                Identifier identifier = (Identifier) token;
                if (!declaredSymbols.contains(identifier.getName())) {
                    result.append("(!) *Undeclared!* ");
                    System.out.println("Undeclared: " + token);
                }
            }
            result.append(token);
        }

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(result.toString()), new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {

            }
        });
    }

    private static Set<Integer> recordStarts(StringBuilder allRules, String regex) {
        Pattern symbolDeclaration = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = symbolDeclaration.matcher(allRules);
        Set<Integer> declarationStarts = new HashSet<Integer>();
        while (matcher.find()) {
            if (matcher.groupCount() > 0) {
                declarationStarts.add(matcher.start(1));
            }
            else {
                declarationStarts.add(matcher.start());
            }
        }
        return declarationStarts;
    }
}
