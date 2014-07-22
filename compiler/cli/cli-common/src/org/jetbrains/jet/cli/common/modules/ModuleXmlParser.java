/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.cli.common.modules;

import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.SmartList;
import kotlin.modules.Module;
import kotlin.modules.ModuleBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.common.messages.MessageCollectorUtil;
import org.jetbrains.jet.cli.common.messages.OutputMessageUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.List;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.ERROR;

public class ModuleXmlParser {

    public static final String MODULES = "modules";
    public static final String MODULE = "module";
    public static final String NAME = "name";
    public static final String OUTPUT_DIR = "outputDir";
    public static final String INCREMENTAL_CACHE = "incrementalCache";
    public static final String SOURCES = "sources";
    public static final String PATH = "path";
    public static final String CLASSPATH = "classpath";
    public static final String EXTERNAL_ANNOTATIONS = "externalAnnotations";

    @NotNull
    public static ModuleScriptData parseModuleScript(
            @NotNull String xmlFile,
            @NotNull MessageCollector messageCollector
    ) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(xmlFile);
            //noinspection IOResourceOpenedButNotSafelyClosed
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
    private String incrementalCacheDir;
    private final List<Module> modules = new SmartList<Module>();
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
            return new ModuleScriptData(modules, incrementalCacheDir);
        }
        catch (ParserConfigurationException e) {
            MessageCollectorUtil.reportException(messageCollector, e);
        }
        catch (SAXException e) {
            messageCollector.report(ERROR, OutputMessageUtil.renderException(e), NO_LOCATION);
        }
        catch (IOException e) {
            MessageCollectorUtil.reportException(messageCollector, e);
        }
        return ModuleScriptData.EMPTY;
    }

    private final DefaultHandler initial = new DefaultHandler() {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (!MODULES.equalsIgnoreCase(qName)) {
                throw createError(qName);
            }

            incrementalCacheDir = attributes.getValue(INCREMENTAL_CACHE);
            setCurrentState(insideModules);
        }
    };

    private final DefaultHandler insideModules = new DefaultHandler() {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (!MODULE.equalsIgnoreCase(qName)) {
                throw createError(qName);
            }

            setCurrentState(new InsideModule(
                    getAttribute(attributes, NAME, qName),
                    getAttribute(attributes, OUTPUT_DIR, qName)
            ));
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (MODULE.equalsIgnoreCase(qName) || MODULES.equalsIgnoreCase(qName)) {
                setCurrentState(insideModules);
            }
        }
    };

    private class InsideModule extends DefaultHandler {

        private final ModuleBuilder moduleBuilder;
        private InsideModule(String name, String outputDir) {
            this.moduleBuilder = new ModuleBuilder(name, outputDir);
            modules.add(moduleBuilder);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (SOURCES.equalsIgnoreCase(qName)) {
                String path = getAttribute(attributes, PATH, qName);
                moduleBuilder.addSourceFiles(path);
            }
            else if (CLASSPATH.equalsIgnoreCase(qName)) {
                String path = getAttribute(attributes, PATH, qName);
                moduleBuilder.addClasspathEntry(path);
            }
            else if (EXTERNAL_ANNOTATIONS.equalsIgnoreCase(qName)) {
                String path = getAttribute(attributes, PATH, qName);
                moduleBuilder.addAnnotationsPathEntry(path);
            }
            else {
                throw createError(qName);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
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

    private static SAXException createError(String qName) throws SAXException {
        return new SAXException("Unexpected tag: " + qName);
    }
}
