/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

{
  "note": "May https://vega.github.io/vega/docs/ be with you",
  "$schema": "https://vega.github.io/schema/vega/v4.3.0.json",
  "description": "TestData geomMean",
  "title": "TestData geomMean",
  "width": 800,
  "height": 500,
  "padding": 5,
  "autosize": {"type": "pad", "resize": true},
  "signals": [
    {
      "name": "clear",
      "value": true,
      "on": [
        {"events": "mouseup[!event.item]", "update": "true", "force": true}
      ]
    },
    {
      "name": "shift",
      "value": false,
      "on": [
        {
          "events": "@legendSymbol:click, @legendLabel:click",
          "update": "event.shiftKey",
          "force": true
        }
      ]
    },
    {
      "name": "clicked",
      "value": null,
      "on": [
        {
          "events": "@legendSymbol:click, @legendLabel:click",
          "comment": "note: here `datum` is `selected` data set",
          "update": "{value: datum.value}",
          "force": true
        }
      ]
    },
    {
      "name": "brush",
      "value": 0,
      "on": [
        {"events": {"signal": "clear"}, "update": "clear ? [0, 0] : brush"},
        {"events": "@xaxis:mousedown", "update": "[x(), x()]"},
        {
          "events": "[@xaxis:mousedown, window:mouseup] > window:mousemove!",
          "update": "[brush[0], clamp(x(), 0, width)]"
        },
        {
          "events": {"signal": "delta"},
          "update": "clampRange([anchor[0] + delta, anchor[1] + delta], 0, width)"
        }
      ]
    },
    {
      "name": "anchor",
      "value": null,
      "on": [{"events": "@brush:mousedown", "update": "slice(brush)"}]
    },
    {
      "name": "xdown",
      "value": 0,
      "on": [{"events": "@brush:mousedown", "update": "x()"}]
    },
    {
      "name": "delta",
      "value": 0,
      "on": [
        {
          "events": "[@brush:mousedown, window:mouseup] > window:mousemove!",
          "update": "x() - xdown"
        }
      ]
    },
    {
      "name": "domain",
      "on": [
        {
          "events": {"signal": "brush"},
          "update": "span(brush) ? invert('x', brush) : null"
        }
      ]
    },
    {"name": "timestamp", "value": true, "bind": {"input": "checkbox"}}
  ],
  "data": [
    {
      "name": "table",
      "comment": "To test chart in VEGA editor https://vega.github.io/editor/#/ change `_values` to `values` and rename `url` property",
      "_values": {
          "hits" : {
            "hits" : [
              {
                "_source" : {"buildId" : 87834896, "buildTimestamp" : "2020-09-21T21:00:31+0000", "geomMean" : 37, "benchmark" : "highlight"}
              },
              {
                "_source" : {"buildId" : 87783396, "buildTimestamp" : "2020-09-21T12:34:19+0000", "geomMean" : 37, "benchmark" : "highlight"}
              },
              {
                "_source" : {"buildId" : 87809918, "buildTimestamp" : "2020-09-21T16:47:11+0000", "geomMean" : 37, "benchmark" : "highlight"}
              },
              {
                "_source" : {"buildId" : 87905203, "buildTimestamp" : "2020-09-22T13:23:44+0000", "geomMean" : 37, "benchmark" : "highlight"}
              },
              {
                "_source" : {"buildId" : 87894638, "buildTimestamp" : "2020-09-22T09:12:16+0000", "geomMean" : 37, "benchmark" : "highlight"}
              }
            ]
          }
      },
      "url": {
        //"comment": "source index pattern",
        "index": "kotlin_ide_benchmarks*",
        //"comment": "it's a body of ES _search query to check query place it into `POST /kotlin_ide_benchmarks*/_search`",
        //"comment": "it uses Kibana specific %timefilter% for time frame selection",
        "body": {
          "size": 1000,
          "query": {
            "bool": {
              "must": [
                {"term": {"benchmark.keyword": "highlight"}},
                {"range": {"buildTimestamp": {"%timefilter%": true}}}
              ]
            }
          },
          "_source": ["buildId", "benchmark", "buildTimestamp", "metrics.hasError", "geomMean"],
          "sort": [{"buildTimestamp": {"order": "asc"}}]
        }
      },
      "format": {"property": "hits.hits"},
      "comment": "we need to have follow data: \"buildId\", \"metricName\", \"metricValue\" and \"metricError\"",
      "comment": "so it has to be array of {\"buildId\": \"...\", \"metricName\": \"...\", \"metricValue\": ..., \"metricError\": ...}",
      "transform": [
        {"type": "collect","sort": {"field": "_source.buildTimestamp"}},
        {
          "comment": "make alias: _source.buildId -> buildId",
          "type": "formula",
          "as": "buildId",
          "expr": "datum._source.buildId"
        },
        {
          "comment": "make alias: _source.benchmark -> metricName",
          "type": "formula",
          "as": "metricName",
          "expr": "datum._source.benchmark"
        },
        {
          "comment": "make alias: _source.geomMean -> metricValue",
          "type": "formula",
          "as": "metricValue",
          "expr": "datum._source.geomMean"
        },
        {
          "comment": "define metricError",
          "type": "formula",
          "as": "metricError",
          "expr": "1"
        },
        {
          "comment": "make alias: _source.metrics[0].hasError -> hasError",
          "type": "formula",
          "as": "hasError",
          "expr": "datum._source.metrics ? datum._source.metrics[0].hasError : false"
        },
        {
          "type": "formula",
          "as": "timestamp",
          "expr": "timeFormat(toDate(datum._source.buildTimestamp), '%Y-%m-%d %H:%M')"
        },
        {
          "comment": "create `url` value that points to TC build",
          "type": "formula",
          "as": "url",
          "expr": "'https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_Benchmarks_PluginPerformanceTests_IdeaPluginPerformanceTests/' + datum._source.buildId"
        }
      ]
    },
    {
      "name": "selected",
      "on": [
        {"trigger": "clear", "remove": true},
        {"trigger": "!shift", "remove": true},
        {"trigger": "!shift && clicked", "insert": "clicked"},
        {"trigger": "shift && clicked", "toggle": "clicked"}
      ]
    }
  ],
  "axes": [
    {
      "scale": "x",
      "grid": true,
      "domain": false,
      "orient": "bottom",
      "labelAngle": -20,
      "labelAlign": "right",
      "title": {"signal": "timestamp ? 'timestamp' : 'buildId'"},
      "titlePadding": 10,
      "tickCount": 5,
      "encode": {
        "labels": {
          "interactive": true,
          "update": {"tooltip": {"signal": "datum.label"}}
        }
      }
    },
    {
      "scale": "y",
      "grid": true,
      "domain": false,
      "orient": "left",
      "titlePadding": 10,
      "title": "ms",
      "titleAnchor": "end",
      "titleAngle": 0
    }
  ],
  "scales": [
    {
      "name": "x",
      "type": "point",
      "range": "width",
      "domain": {"data": "table", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}}
    },
    {
      "name": "y",
      "type": "linear",
      "range": "height",
      "nice": true,
      "zero": true,
      "domain": {"data": "table", "field": "metricValue"}
    },
    {
      "name": "color",
      "type": "ordinal",
      "range": "category",
      "domain": {"data": "table", "field": "metricName"}
    },
    {
      "name": "size",
      "type": "linear",
      "round": true,
      "nice": false,
      "zero": true,
      "domain": {"data": "table", "field": "metricError"},
      "range": [1, 100]
    }
  ],
  "legends": [
    {
      "title": "Cases",
      "stroke": "color",
      "strokeColor": "#ccc",
      "padding": 8,
      "cornerRadius": 4,
      "symbolLimit": 50,
      "encode": {
        "symbols": {
          "name": "legendSymbol",
          "interactive": true,
          "update": {
            "fill": {"value": "transparent"},
            "strokeWidth": {"value": 2},
            "opacity": [
              {
                "comment": "here `datum` is `selected` data set",
                "test": "!length(data('selected')) || indata('selected', 'value', datum.value)",
                "value": 0.7
              },
              {"value": 0.15}
            ],
            "size": {"value": 64}
          }
        },
        "labels": {
          "name": "legendLabel",
          "interactive": true,
          "update": {
            "opacity": [
              {
                "comment": "here `datum` is `selected` data set",
                "test": "!length(data('selected')) || indata('selected', 'value', datum.value)",
                "value": 1
              },
              {"value": 0.25}
            ]
          }
        }
      }
    }
  ],
  "marks": [
    {
      "type": "group",
      "from": {
        "facet": {"name": "series", "data": "table", "groupby": "metricName"}
      },
      "marks": [
        {
          "type": "line",
          "from": {"data": "series"},
          "encode": {
            "hover": {"opacity": {"value": 1}, "strokeWidth": {"value": 4}},
            "update": {
              "x": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}},
              "y": {"scale": "y", "field": "metricValue"},
              "strokeWidth": {"value": 2},
              "opacity": [
                {
                  "test": "(!domain || inrange(datum.buildId, domain)) && (!length(data('selected')) || indata('selected', 'value', datum.metricName))",
                  "value": 0.7
                },
                {"value": 0.15}
              ],
              "stroke": [
                {
                  "test": "(!domain || inrange(datum.buildId, domain)) && (!length(data('selected')) || indata('selected', 'value', datum.metricName))",
                  "scale": "color",
                  "field": "metricName"
                },
                {"value": "#ccc"}
              ]
            }
          }
        },
        {
          "type": "symbol",
          "from": {"data": "series"},
          "encode": {
            "enter": {
              "fill": {"value": "#B00"},
              "size": [{ "test": "datum.hasError", "value": 250 }, {"value": 0}],
              "shape": {"value": "cross"},
              "angle": {"value": 45},
              "x": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}},
              "y": {"scale": "y", "field": "metricValue"},
              "strokeWidth": {"value": 1},
              "opacity": [
                {
                  "test": "(!domain || inrange(datum.buildId, domain)) && datum.hasError && (!length(data('selected')) || indata('selected', 'value', datum.metricName))",
                  "value": 1
                },
                {"value": 0.15}
              ]
            },
            "update": {
              "opacity": [
                {
                  "test": "(!domain || inrange(datum.buildId, domain))  && datum.hasError && (!length(data('selected')) || indata('selected', 'value', datum.metricName))",
                  "value": 1
                },
                {"value": 0.15}
              ]
            }
          },
          "zindex": 1
        },
        {
          "type": "symbol",
          "from": {"data": "series"},
          "encode": {
            "enter": {
              "tooltip": {
                "signal": "datum.metricName + ': ' + datum.metricValue + ' ms'"
              },
              "href": {"field": "url"},
              "cursor": {"value": "pointer"},
              "size": {"scale": "size", "field": "metricError"},
              "x": {"scale": "x", "field": {"signal": "timestamp ? 'timestamp' : 'buildId'"}},
              "y": {"scale": "y", "field": "metricValue"},
              "strokeWidth": {"value": 1},
              "fill": {"scale": "color", "field": "metricName"}
            },
            "update": {
              "opacity": [
                {
                  "test": "(!domain || inrange(datum.buildId, domain)) && (!length(data('selected')) || indata('selected', 'value', datum.metricName))",
                  "value": 1
                },
                {"value": 0.15}
              ]
            }
          },
          "zindex": 2
        }
      ]
    }
  ]
}
