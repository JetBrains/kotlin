/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

{
  "note": "May https://vega.github.io/vega/docs/ be with you",
  "$schema": "https://vega.github.io/schema/vega/v4.3.0.json",
  "description": "TestData highlight",
  "title": "TestData highlight",
  "width": 800,
  "height": 400,
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
        "aggregations" : {
          "benchmark" : {
            "doc_count_error_upper_bound" : 0,
            "sum_other_doc_count" : 0,
            "buckets" : [
              {
                "key" : "highlight",
                "doc_count" : 1034,
                "name" : {
                  "doc_count_error_upper_bound" : 0,
                  "sum_other_doc_count" : 0,
                  "buckets" : [
                    {
                      "key" : "Annotations",
                      "doc_count" : 23,
                      "values" : {
                        "buckets" : [
                          {
                            "key_as_string" : "2020-11-02T09:00:00.000Z",
                            "key" : 1604307600000,
                            "doc_count" : 3,
                            "avgError" : {
                              "value" : 1.0
                            },
                            "avgValue" : {
                              "value" : 129.0
                            },
                            "buildId" : {
                              "doc_count_error_upper_bound" : 0,
                              "sum_other_doc_count" : 1,
                              "buckets" : [
                                {
                                  "key" : 93277596,
                                  "doc_count" : 2
                                }
                              ]
                            }
                          },
                          {
                            "key_as_string" : "2020-11-02T21:00:00.000Z",
                            "key" : 1604350800000,
                            "doc_count" : 3,
                            "avgError" : {
                              "value" : 1.0
                            },
                            "avgValue" : {
                              "value" : 41.0
                            },
                            "buildId" : {
                              "doc_count_error_upper_bound" : 0,
                              "sum_other_doc_count" : 2,
                              "buckets" : [
                                {
                                  "key" : 93400366,
                                  "doc_count" : 1
                                }
                              ]
                            }
                          },
                          {
                            "key_as_string" : "2020-11-03T09:00:00.000Z",
                            "key" : 1604394000000,
                            "doc_count" : 2,
                            "avgError" : {
                              "value" : 1.5
                            },
                            "avgValue" : {
                              "value" : 42.0
                            },
                            "buildId" : {
                              "doc_count_error_upper_bound" : 0,
                              "sum_other_doc_count" : 1,
                              "buckets" : [
                                {
                                  "key" : 93507855,
                                  "doc_count" : 1
                                }
                              ]
                            }
                          }
                        ],
                        "interval" : "12h"
                      }
                    }
                  ]
                }
              }
            ]
          }
        }
      },
      "url": {
        //"comment": "source index pattern",
        "index": "kotlin_ide_benchmarks*",
        //"comment": "it's a body of ES _search query to check query place it into `POST /kotlin_ide_benchmarks*/_search`",
        //"comment": "it uses Kibana specific %timefilter% for time frame selection",
        "body": {
          "size": 0,
          "query": {
            "bool": {
              "must": [
                {
                  "bool": {
                    "must_not": [
                       {"exists": {"field": "warmUp"}},
                       {"exists": {"field": "synthetic"}}
                     ]
                   }
                },
                {"term": {"benchmark.keyword": "highlight"}},
                {"range": {"buildTimestamp": {"%timefilter%": true}}}
              ]
            }
          },
          "aggs": {
            "benchmark": {
              "terms": {
                "field": "benchmark.keyword",
                "size": 500
              },
              "aggs": {
                "name": {
                  "terms": {
                    "field": "name.keyword",
                    "size": 500
                  },
                  "aggs": {
                    "values": {
                      "auto_date_histogram": {
                          "buckets": 500,
                          "field": "buildTimestamp",
                          "minimum_interval": "hour"
                      },
                      "aggs": {
                        "buildId": {
                          "terms": {
                            "size": 1,
                            "field": "buildId"
                          }
                        },
                        "avgValue":{
                          "avg": {
                            "field": "metricValue"
                          }
                        },
                        "avgError":{
                          "avg": {
                            "field": "metricError"
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      },
      "format": {"property": "aggregations"},
      "comment": "we need to have follow data: \"buildId\", \"metricName\", \"metricValue\" and \"metricError\"",
      "comment": "so it has to be array of {\"buildId\": \"...\", \"metricName\": \"...\", \"metricValue\": ..., \"metricError\": ...}",
      "transform": [
        {"type": "project", "fields": ["benchmark"]},
        {"type": "flatten", "fields": ["benchmark.buckets"], "as": ["benchmark_buckets"]},
        {"type": "project", "fields": ["benchmark_buckets.key", "benchmark_buckets.name"], "as": ["benchmark", "benchmark_buckets_name"]},
        {"type": "flatten", "fields": ["benchmark_buckets_name.buckets"], "as": ["name_buckets"]},
        {"type": "project", "fields": ["benchmark", "name_buckets.key", "name_buckets.values"], "as": ["benchmark", "name", "name_values"]},
        {"type": "flatten", "fields": ["name_values.buckets"], "as": ["name_values_buckets"]},
        {"type": "project", "fields": ["benchmark", "name", "name_values_buckets.key", "name_values_buckets.key_as_string", "name_values_buckets.avgError", "name_values_buckets.avgValue", "name_values_buckets.buildId.buckets"], "as": ["benchmark", "metricName", "buildTimestamp", "timestamp_value", "avgError", "avgValue", "buildId_buckets"]},
        {"type": "formula", "as": "metricError", "expr": "datum.avgError.value"},
        {"type": "formula", "as": "metricValue", "expr": "datum.avgValue.value"},
        {"type": "flatten", "fields": ["buildId_buckets"], "as": ["buildId_values"]},
        {"type": "formula", "as": "buildId", "expr": "datum.buildId_values.key"},
        {
          "type": "formula",
          "as": "timestamp",
          "expr": "timeFormat(toDate(datum.buildTimestamp), '%Y-%m-%d %H:%M')"
        },
        {
          "comment": "create `url` value that points to TC build",
          "type": "formula",
          "as": "url",
          "expr": "'https://buildserver.labs.intellij.net/buildConfiguration/Kotlin_Benchmarks_PluginPerformanceTests_IdeaPluginPerformanceTests/' + datum.buildId"
        },
        {"type": "collect","sort": {"field": "timestamp"}}
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
