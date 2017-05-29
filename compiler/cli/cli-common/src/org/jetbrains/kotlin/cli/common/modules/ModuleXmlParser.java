/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common.modules;

import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil;
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil;
import org.jetbrains.kotlin.modules.JavaRootPath;
import org.jetbrains.kotlin.modules.Module;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.List;

import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR;

public class ModuleXmlParser {

    public static final String MODULES = "modules";
    public static final String MODULE = "module";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String TYPE_PRODUCTION = "java-production";
    public static final String TYPE_TEST = "java-test";
    public static final String OUTPUT_DIR = "outputDir";
    public static final String FRIEND_DIR = "friendDir";
    public static final String SOURCES = "sources";
    public static final String JAVA_SOURCE_ROOTS = "javaSourceRoots";
    public static final String JAVA_SOURCE_PACKAGE_PREFIX = "packagePrefix";
    public static final String PATH = "path";
    public static final String CLASSPATH = "classpath";

    @NotNull
    public static ModuleScriptData parseModuleScript(
            @NotNull String xmlFile,
            @NotNull MessageCollector messageCollector
    ) {
        FileInputStream stream = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            stream = new FileInputStream(xmlFile);
            return new ModuleXmlParser(messageCollector).parse(new BufferedInputStream(stream));
        }
        catch (FileNotFoundException e) {
            MessageCollectorUtil.reportException(messageCollector, e);
            return ModuleScriptData.EMPTY;
        }
        finally {
            StreamUtil.closeStream(stream);
        }
    }

    private final MessageCollector messageCollector;
    private final List<Module> modules = new SmartList<>();
    private DefaultHandler currentState;

    private ModuleXmlParser(@NotNull MessageCollector messageCollector) {
        this.messageCollector = messageCollector;
    }

    private void setCurrentState(@NotNull DefaultHandler currentState) {
        this.currentState = currentState;
    }

    private ModuleScriptData parse(@NotNull InputStream xml) {
        try {
            setCurrentState(initial);
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(xml, new DelegatedSaxHandler() {
                @NotNull
                @Override
                protected DefaultHandler getDelegate() {
                    return currentState;
                }
            });
            return new ModuleScriptData(modules);
        }
        catch (ParserConfigurationException | IOException e) {
            MessageCollectorUtil.reportException(messageCollector, e);
        }
        catch (SAXException e) {
            messageCollector.report(ERROR, OutputMessageUtil.renderException(e), null);
        }
        return ModuleScriptData.EMPTY;
    }

    private final DefaultHandler initial = new DefaultHandler() {
        @Override
        public void startElement(@NotNull String uri, @NotNull String localName, @NotNull String qName, @NotNull Attributes attributes)
                throws SAXException {
            if (!MODULES.equalsIgnoreCase(qName)) {
                throw createError(qName);
            }

            setCurrentState(insideModules);
        }
    };

    private final DefaultHandler insideModules = new DefaultHandler() {
        @Override
        public void startElement(@NotNull String uri, @NotNull String localName, @NotNull String qName, @NotNull Attributes attributes)
                throws SAXException {
            if (!MODULE.equalsIgnoreCase(qName)) {
                throw createError(qName);
            }

            String moduleType = getAttribute(attributes, TYPE, qName);
            assert(TYPE_PRODUCTION.equals(moduleType) || TYPE_TEST.equals(moduleType)): "Unknown module type: " + moduleType;
            setCurrentState(new InsideModule(
                    getAttribute(attributes, NAME, qName),
                    getAttribute(attributes, OUTPUT_DIR, qName),
                    moduleType
            ));
        }

        @Override
        public void endElement(String uri, @NotNull String localName, @NotNull String qName) throws SAXException {
            if (MODULE.equalsIgnoreCase(qName) || MODULES.equalsIgnoreCase(qName)) {
                setCurrentState(insideModules);
            }
        }
    };

    private class InsideModule extends DefaultHandler {

        private final ModuleBuilder moduleBuilder;
        private InsideModule(String name, String outputDir, @NotNull String type) {
            this.moduleBuilder = new ModuleBuilder(name, outputDir, type);
            modules.add(moduleBuilder);
        }

        @Override
        public void startElement(@NotNull String uri, @NotNull String localName, @NotNull String qName, @NotNull Attributes attributes)
                throws SAXException {
            if (SOURCES.equalsIgnoreCase(qName)) {
                String path = getAttribute(attributes, PATH, qName);
                moduleBuilder.addSourceFiles(path);
            }
            else if (FRIEND_DIR.equalsIgnoreCase(qName)) {
                String path = getAttribute(attributes, PATH, qName);
                moduleBuilder.addFriendDir(path);
            }
            else if (CLASSPATH.equalsIgnoreCase(qName)) {
                String path = getAttribute(attributes, PATH, qName);
                moduleBuilder.addClasspathEntry(path);
            }
            else if (JAVA_SOURCE_ROOTS.equalsIgnoreCase(qName)) {
                String path = getAttribute(attributes, PATH, qName);
                String packagePrefix = getNullableAttribute(attributes, JAVA_SOURCE_PACKAGE_PREFIX);
                moduleBuilder.addJavaSourceRoot(new JavaRootPath(path, packagePrefix));
            }
            else {
                throw createError(qName);
            }
        }

        @Override
        public void endElement(String uri, @NotNull String localName, @NotNull String qName) throws SAXException {
            if (MODULE.equalsIgnoreCase(qName)) {
                setCurrentState(insideModules);
            }
        }
    }

    @NotNull
    private static String getAttribute(Attributes attributes, String qName, String tag) throws SAXException {
        String name = attributes.getValue(qName);
        if (name == null) {
            throw new SAXException("No '" + qName + "' attribute for " + tag);
        }
        return name;
    }

    @Nullable
    private static String getNullableAttribute(Attributes attributes, String qName) throws SAXException {
        return attributes.getValue(qName);
    }


    private static SAXException createError(String qName) throws SAXException {
        return new SAXException("Unexpected tag: " + qName);
    }
}
